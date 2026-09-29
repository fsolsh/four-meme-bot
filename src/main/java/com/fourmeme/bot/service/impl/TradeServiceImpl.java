package com.fourmeme.bot.service.impl;

import com.fourmeme.bot.entity.BotTask;
import com.fourmeme.bot.entity.Wallet;
import com.fourmeme.bot.enums.MarketPair;
import com.fourmeme.bot.service.CredentialsService;
import com.fourmeme.bot.service.TradeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.tx.FastRawTransactionManager;
import org.web3j.utils.Numeric;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class TradeServiceImpl implements TradeService {

    private final Web3j web3j;
    private final CredentialsService credentialsService;   // ✅ 改这里
    private final long chainId;

    @Value("${contract.four-meme-proxy}")
    private String fourMemeProxy;

    @Value("${contract.pancake-router-v2}")
    private String pancakeRouterV2;

    @Value("${contract.pancake-factory-v2}")
    private String pancakeFactoryV2;

    @Value("${bot.default.gas-limit.buy}")
    private long buyGasLimit;

    @Value("${bot.default.gas-limit.sell}")
    private long sellGasLimit;

    private final Random random = new Random();

    private static final String ZERO_ADDRESS = "0x0000000000000000000000000000000000000000";
    private static final BigInteger MAX_UINT256 =
            new BigInteger("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff", 16);
    private static final int TOKEN_DECIMALS = 18;

    // ==================== 毕业前：Four.meme 联合曲线 ====================

    @Override
    public String buyOnBondingCurve(Wallet wallet, BotTask task,
                                    BigDecimal amount, BigInteger gasPrice) throws Exception {
        Credentials credentials = credentialsService.getCredentials(wallet);
        MarketPair market = MarketPair.fromName(task.getMarketPair());

        if (market.isNativeToken()) {
            BigInteger amountWei = amount.multiply(
                    BigDecimal.TEN.pow(market.getDecimals())).toBigInteger();

            Function function = new Function(
                    market.getBuyFunctionName(),
                    Arrays.asList(
                            new Address(task.getTokenAddress()),
                            new Address(wallet.getAddress()),
                            new Uint256(BigInteger.ONE),
                            new Address(ZERO_ADDRESS),
                            new Uint256(BigInteger.ZERO)
                    ),
                    Collections.emptyList()
            );

            String encoded = FunctionEncoder.encode(function);
            return sendTransaction(credentials, fourMemeProxy,
                    amountWei, encoded, gasPrice, buyGasLimit);

        } else {
            approveToken(credentials, market.getQuoteTokenAddress(), fourMemeProxy, gasPrice);

            BigInteger amountWei = amount.multiply(
                    BigDecimal.TEN.pow(market.getDecimals())).toBigInteger();

            Function function = new Function(
                    market.getBuyFunctionName(),
                    Arrays.asList(
                            new Address(task.getTokenAddress()),
                            new Address(market.getQuoteTokenAddress()),
                            new Uint256(amountWei),
                            new Address(wallet.getAddress()),
                            new Uint256(BigInteger.ONE)
                    ),
                    Collections.emptyList()
            );

            String encoded = FunctionEncoder.encode(function);
            return sendTransaction(credentials, fourMemeProxy,
                    BigInteger.ZERO, encoded, gasPrice, buyGasLimit);
        }
    }

    @Override
    public String sellOnBondingCurve(Wallet wallet, BotTask task,
                                     BigDecimal tokenAmount, BigInteger gasPrice) throws Exception {
        Credentials credentials = credentialsService.getCredentials(wallet);
        MarketPair market = MarketPair.fromName(task.getMarketPair());

        approveToken(credentials, task.getTokenAddress(), fourMemeProxy, gasPrice);

        BigInteger tokenWei = tokenAmount.multiply(
                BigDecimal.TEN.pow(TOKEN_DECIMALS)).toBigInteger();

        if (market.isNativeToken()) {
            Function function = new Function(
                    market.getSellFunctionName(),
                    Arrays.asList(
                            new Address(task.getTokenAddress()),
                            new Address(wallet.getAddress()),
                            new Uint256(tokenWei),
                            new Uint256(BigInteger.ONE)
                    ),
                    Collections.emptyList()
            );
            String encoded = FunctionEncoder.encode(function);
            return sendTransaction(credentials, fourMemeProxy,
                    BigInteger.ZERO, encoded, gasPrice, sellGasLimit);

        } else {
            Function function = new Function(
                    market.getSellFunctionName(),
                    Arrays.asList(
                            new Address(task.getTokenAddress()),
                            new Address(market.getQuoteTokenAddress()),
                            new Uint256(tokenWei),
                            new Address(wallet.getAddress()),
                            new Uint256(BigInteger.ONE)
                    ),
                    Collections.emptyList()
            );
            String encoded = FunctionEncoder.encode(function);
            return sendTransaction(credentials, fourMemeProxy,
                    BigInteger.ZERO, encoded, gasPrice, sellGasLimit);
        }
    }

    // ==================== 毕业后：PancakeSwap V2 ====================

    @Override
    public String buyOnPancake(Wallet wallet, BotTask task,
                               BigDecimal amount, BigInteger gasPrice) throws Exception {
        Credentials credentials = credentialsService.getCredentials(wallet);
        MarketPair market = MarketPair.fromName(task.getMarketPair());
        long deadline = System.currentTimeMillis() / 1000 + 600;

        if (market.isNativeToken()) {
            BigInteger amountWei = amount.multiply(
                    BigDecimal.TEN.pow(market.getDecimals())).toBigInteger();

            List<Address> path = Arrays.asList(
                    new Address(market.getPancakeQuoteTokenAddress()),
                    new Address(task.getTokenAddress())
            );

            Function function = new Function(
                    "swapExactETHForTokens",
                    Arrays.asList(
                            new Uint256(BigInteger.ONE),
                            new org.web3j.abi.datatypes.DynamicArray<>(Address.class, path),
                            new Address(wallet.getAddress()),
                            new Uint256(BigInteger.valueOf(deadline))
                    ),
                    Collections.emptyList()
            );

            String encoded = FunctionEncoder.encode(function);
            return sendTransaction(credentials, pancakeRouterV2,
                    amountWei, encoded, gasPrice, buyGasLimit);

        } else {
            approveToken(credentials, market.getQuoteTokenAddress(), pancakeRouterV2, gasPrice);

            BigInteger amountWei = amount.multiply(
                    BigDecimal.TEN.pow(market.getDecimals())).toBigInteger();

            List<Address> path = Arrays.asList(
                    new Address(market.getPancakeQuoteTokenAddress()),
                    new Address(task.getTokenAddress())
            );

            Function function = new Function(
                    "swapExactTokensForTokens",
                    Arrays.asList(
                            new Uint256(amountWei),
                            new Uint256(BigInteger.ONE),
                            new org.web3j.abi.datatypes.DynamicArray<>(Address.class, path),
                            new Address(wallet.getAddress()),
                            new Uint256(BigInteger.valueOf(deadline))
                    ),
                    Collections.emptyList()
            );

            String encoded = FunctionEncoder.encode(function);
            return sendTransaction(credentials, pancakeRouterV2,
                    BigInteger.ZERO, encoded, gasPrice, buyGasLimit);
        }
    }

    @Override
    public String sellOnPancake(Wallet wallet, BotTask task,
                                BigDecimal tokenAmount, BigInteger gasPrice) throws Exception {
        Credentials credentials = credentialsService.getCredentials(wallet);
        MarketPair market = MarketPair.fromName(task.getMarketPair());

        approveToken(credentials, task.getTokenAddress(), pancakeRouterV2, gasPrice);

        BigInteger tokenWei = tokenAmount.multiply(
                BigDecimal.TEN.pow(TOKEN_DECIMALS)).toBigInteger();
        long deadline = System.currentTimeMillis() / 1000 + 600;

        if (market.isNativeToken()) {
            List<Address> path = Arrays.asList(
                    new Address(task.getTokenAddress()),
                    new Address(market.getPancakeQuoteTokenAddress())
            );

            Function function = new Function(
                    "swapExactTokensForETH",
                    Arrays.asList(
                            new Uint256(tokenWei),
                            new Uint256(BigInteger.ONE),
                            new org.web3j.abi.datatypes.DynamicArray<>(Address.class, path),
                            new Address(wallet.getAddress()),
                            new Uint256(BigInteger.valueOf(deadline))
                    ),
                    Collections.emptyList()
            );

            String encoded = FunctionEncoder.encode(function);
            return sendTransaction(credentials, pancakeRouterV2,
                    BigInteger.ZERO, encoded, gasPrice, sellGasLimit);

        } else {
            List<Address> path = Arrays.asList(
                    new Address(task.getTokenAddress()),
                    new Address(market.getPancakeQuoteTokenAddress())
            );

            Function function = new Function(
                    "swapExactTokensForTokens",
                    Arrays.asList(
                            new Uint256(tokenWei),
                            new Uint256(BigInteger.ONE),
                            new org.web3j.abi.datatypes.DynamicArray<>(Address.class, path),
                            new Address(wallet.getAddress()),
                            new Uint256(BigInteger.valueOf(deadline))
                    ),
                    Collections.emptyList()
            );

            String encoded = FunctionEncoder.encode(function);
            return sendTransaction(credentials, pancakeRouterV2,
                    BigInteger.ZERO, encoded, gasPrice, sellGasLimit);
        }
    }

    // ==================== 交易调度与授权 ====================

    @Override
    public String executeTrade(Wallet wallet, BotTask task,
                               boolean isBuy, BigDecimal amount,
                               BigInteger gasPrice) throws Exception {
        if (task.getGraduated() == 0) {
            return isBuy
                    ? buyOnBondingCurve(wallet, task, amount, gasPrice)
                    : sellOnBondingCurve(wallet, task, amount, gasPrice);
        } else {
            return isBuy
                    ? buyOnPancake(wallet, task, amount, gasPrice)
                    : sellOnPancake(wallet, task, amount, gasPrice);
        }
    }

    @Override
    public String approveToken(Credentials credentials, String tokenAddress,
                               String spender, BigInteger gasPrice) throws Exception {
        Function function = new Function(
                "approve",
                Arrays.asList(
                        new Address(spender),
                        new Uint256(MAX_UINT256)
                ),
                Collections.emptyList()
        );
        String encoded = FunctionEncoder.encode(function);
        return sendTransaction(credentials, tokenAddress,
                BigInteger.ZERO, encoded, gasPrice, 100000);
    }

    @Override
    public BigDecimal getErc20Balance(String tokenAddress, String walletAddress, Integer decimals) {
        try {
            Function function = new Function(
                    "balanceOf",
                    Collections.singletonList(new Address(walletAddress)),
                    Collections.singletonList(new TypeReference<Uint256>() { })
            );
            String encoded = FunctionEncoder.encode(function);

            EthCall response = web3j.ethCall(
                    Transaction.createEthCallTransaction(null, tokenAddress, encoded),
                    DefaultBlockParameterName.LATEST
            ).send();

            if (response.hasError()) {
                log.debug("balanceOf 调用失败 {}: {}", tokenAddress, response.getError().getMessage());
                return BigDecimal.ZERO;
            }

            List<Type> results = FunctionReturnDecoder.decode(
                    response.getValue(), function.getOutputParameters());
            if (results.isEmpty() || results.get(0).getValue() == null) {
                return BigDecimal.ZERO;
            }

            BigInteger raw = (BigInteger) results.get(0).getValue();
            int dec = (decimals != null && decimals > 0) ? decimals : 18;
            return new BigDecimal(raw).divide(BigDecimal.TEN.pow(dec), dec, RoundingMode.DOWN);

        } catch (Exception e) {
            log.debug("查询代币余额异常 {}: {}", tokenAddress, e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    // ==================== 毕业检测 ====================

    @Override
    public boolean isTokenOnPancakeSwap(String tokenAddress, String quoteTokenAddress) {
        try {
            Function function = new Function(
                    "getPair",
                    Arrays.asList(
                            new Address(tokenAddress),
                            new Address(quoteTokenAddress)
                    ),
                    List.of(new TypeReference<Address>() { })
            );
            String encoded = FunctionEncoder.encode(function);

            EthCall response = web3j.ethCall(
                    Transaction.createEthCallTransaction(null, pancakeFactoryV2, encoded),
                    DefaultBlockParameterName.LATEST
            ).send();

            if (response.hasError()) {
                log.warn("getPair 调用失败: {}", response.getError().getMessage());
                return false;
            }

            List<Type> results = FunctionReturnDecoder.decode(response.getValue(), function.getOutputParameters());

            if (results.isEmpty()) return false;
            String pairAddress = results.get(0).getValue().toString();
            return !pairAddress.equals(ZERO_ADDRESS);

        } catch (Exception e) {
            log.warn("查询PancakeSwap交易对失败 token={} quote={}: {}", tokenAddress, quoteTokenAddress, e.getMessage());
            return false;
        }
    }

    public boolean isTokenOnPancakeSwap(BotTask task) {
        MarketPair market = MarketPair.fromName(task.getMarketPair());
        return isTokenOnPancakeSwap(task.getTokenAddress(),
                market.getPancakeQuoteTokenAddress());
    }

    // ==================== Gas 价格 ====================

    @Override
    public BigInteger getRandomGasPrice() {
        final BigInteger MIN_GAS_PRICE = BigInteger.valueOf(100_000_000L);
        final BigInteger FALLBACK_GAS_PRICE = BigInteger.valueOf(1_000_000_000L);

        try {
            BigInteger baseGas = web3j.ethGasPrice().send().getGasPrice();

            if (baseGas.compareTo(MIN_GAS_PRICE) < 0) {
                baseGas = MIN_GAS_PRICE;
            }

            double factor = 1.0 + random.nextDouble() * 0.2;
            BigInteger gasPrice = BigDecimal.valueOf(baseGas.doubleValue() * factor).toBigInteger();

            return gasPrice.max(MIN_GAS_PRICE);

        } catch (Exception e) {
            log.debug("获取 gas price 失败，使用兜底值: {}", e.getMessage());
            return FALLBACK_GAS_PRICE;
        }
    }

    // ==================== 代币元数据 ====================

    @Override
    public String getTokenSymbol(String tokenAddress) {
        return getStringField(tokenAddress, "symbol");
    }

    @Override
    public String getTokenName(String tokenAddress) {
        return getStringField(tokenAddress, "name");
    }

    @Override
    public Integer getTokenDecimals(String tokenAddress) {
        return readUint8Field(tokenAddress, "decimals");
    }

    @Override
    public MarketPair detectMarket(String tokenAddress) {
        String addr = tokenAddress.toLowerCase();
        for (MarketPair market : MarketPair.values()) {
            if (isTokenOnPancakeSwap(addr, market.getPancakeQuoteTokenAddress())) {
                return market;
            }
        }
        return queryMarketFromFourMeme(addr);
    }

    // ==================== 私有方法：代币字段读取 ====================

    private String getStringField(String tokenAddress, String method) {
        try {
            String rawHex = callRaw(tokenAddress, method);
            if (rawHex == null || rawHex.isEmpty()) {
                log.debug("{}() 返回空: {}", method, tokenAddress);
                return null;
            }

            String decoded = decodeAsString(rawHex);
            if (decoded != null && !decoded.isEmpty()) {
                log.debug("成功获取 {} (string): {}", method, decoded);
                return decoded;
            }

            decoded = decodeAsBytes32(rawHex);
            if (decoded != null && !decoded.isEmpty()) {
                log.debug("成功获取 {} (bytes32): {}", method, decoded);
                return decoded;
            }

            log.warn("{}() 解码失败: {} raw={}", method, tokenAddress, rawHex);
            return null;

        } catch (Exception e) {
            log.error("查询代币 {} 失败: {}", method, tokenAddress, e);
            return null;
        }
    }

    private String callRaw(String tokenAddress, String method) {
        try {
            Function function = new Function(
                    method,
                    Collections.emptyList(),
                    Collections.emptyList()
            );
            String encoded = FunctionEncoder.encode(function);

            EthCall response = web3j.ethCall(
                    Transaction.createEthCallTransaction(null, tokenAddress, encoded),
                    DefaultBlockParameterName.LATEST
            ).send();

            if (response.hasError()) {
                log.debug("ethCall {}() 返回错误: {}", method, response.getError().getMessage());
                return null;
            }

            String value = response.getValue();
            if (value == null || value.equals("0x")) {
                return null;
            }
            return value.startsWith("0x") ? value.substring(2) : value;

        } catch (Exception e) {
            log.debug("callRaw {}() {} 失败: {}", method, tokenAddress, e.getMessage());
            return null;
        }
    }

    private String decodeAsString(String hex) {
        if (hex == null || hex.length() < 128) {
            return null;
        }
        try {
            String offsetHex = hex.substring(0, 64);
            String lengthHex = hex.substring(64, 128);

            BigInteger offset = new BigInteger(offsetHex, 16);
            if (!offset.equals(BigInteger.valueOf(32))) {
                return null;
            }

            BigInteger length = new BigInteger(lengthHex, 16);
            int len = length.intValue();
            if (len <= 0 || len > 200) {
                return null;
            }

            int dataEnd = 128 + len * 2;
            if (hex.length() < dataEnd) {
                return null;
            }

            String dataHex = hex.substring(128, dataEnd);
            byte[] data = Numeric.hexStringToByteArray(dataHex);
            return new String(data, StandardCharsets.UTF_8).trim();

        } catch (Exception e) {
            log.debug("decodeAsString 失败: {}", e.getMessage());
            return null;
        }
    }

    private String decodeAsBytes32(String hex) {
        if (hex == null || hex.length() < 64) {
            return null;
        }
        try {
            String dataHex = hex.substring(0, 64);
            byte[] data = Numeric.hexStringToByteArray(dataHex);

            int end = 0;
            while (end < data.length && data[end] != 0) {
                end++;
            }
            if (end == 0) {
                return null;
            }

            byte[] trimmed = new byte[end];
            System.arraycopy(data, 0, trimmed, 0, end);

            try {
                return new String(trimmed, StandardCharsets.UTF_8).trim();
            } catch (Exception e) {
                return new String(trimmed, StandardCharsets.ISO_8859_1).trim();
            }

        } catch (Exception e) {
            log.debug("decodeAsBytes32 失败: {}", e.getMessage());
            return null;
        }
    }

    private Integer readUint8Field(String tokenAddress, String method) {
        try {
            Function function = new Function(method, Collections.emptyList(),
                    List.of(new TypeReference<Uint8>() { }));
            String encoded = FunctionEncoder.encode(function);
            EthCall response = web3j.ethCall(
                    Transaction.createEthCallTransaction(null, tokenAddress, encoded),
                    DefaultBlockParameterName.LATEST
            ).send();
            if (response.hasError()) {
                return null;
            }
            List<Type> results = FunctionReturnDecoder.decode(
                    response.getValue(), function.getOutputParameters());
            if (results.isEmpty()) return null;
            Object value = results.get(0).getValue();
            return value != null ? ((BigInteger) value).intValue() : null;
        } catch (Exception e) {
            log.debug("readUint8Field {}() {} 失败: {}", method, tokenAddress, e.getMessage());
            return null;
        }
    }

    private MarketPair queryMarketFromFourMeme(String tokenAddress) {
        try {
            Function function = new Function(
                    "tokens",
                    List.of(new Address(tokenAddress)),
                    Arrays.asList(
                            new TypeReference<Address>() { },
                            new TypeReference<Uint256>() { },
                            new TypeReference<Uint256>() { }
                    )
            );
            String encoded = FunctionEncoder.encode(function);
            EthCall response = web3j.ethCall(
                    Transaction.createEthCallTransaction(null, fourMemeProxy, encoded),
                    DefaultBlockParameterName.LATEST
            ).send();

            List<Type> results = FunctionReturnDecoder.decode(
                    response.getValue(), function.getOutputParameters());
            if (results.isEmpty()) return null;

            String quoteToken = results.get(0).getValue().toString();
            return MarketPair.fromQuoteTokenAddress(quoteToken);

        } catch (Exception e) {
            log.debug("从Four.meme查询市场失败: {}", e.getMessage());
            return null;
        }
    }

    // ==================== 私有方法：交易发送 ====================

    private String sendTransaction(Credentials credentials, String to,
                                   BigInteger value, String data,
                                   BigInteger gasPrice, long gasLimit) throws Exception {
        FastRawTransactionManager txManager =
                new FastRawTransactionManager(web3j, credentials, chainId);

        EthSendTransaction response = txManager.sendTransaction(
                gasPrice,
                BigInteger.valueOf(gasLimit),
                to,
                data,
                value
        );

        if (response.hasError()) {
            throw new RuntimeException("交易失败: " + response.getError().getMessage());
        }

        return response.getTransactionHash();
    }
}