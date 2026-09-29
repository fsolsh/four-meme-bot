package com.fourmeme.bot.enums;

import lombok.Getter;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Uint;
import org.web3j.abi.datatypes.Address;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.utils.Numeric;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Collections;

@Getter
public enum MarketPair {

    BNB(
            "BNB",
            "0x0000000000000000000000000000000000000000",
            "0xbb4CdB9CBd36B01bD1cBaEBF2De08d9173bc095c",
            18,
            BigDecimal.valueOf(18),
            "buy",
            "sell",
            true
    ),

    USDT(
            "USDT",
            "0x55d398326f99059fF775485246999027B3197955",
            "0x55d398326f99059fF775485246999027B3197955",
            18,
            BigDecimal.valueOf(12000),
            "buyWithERC20",
            "sellWithERC20",
            false
    );

    private final String name;
    private final String quoteTokenAddress;
    private final String pancakeQuoteTokenAddress;
    private final int decimals;
    private final BigDecimal defaultGraduationThreshold;
    private final String buyFunctionName;
    private final String sellFunctionName;
    private final boolean nativeToken;

    MarketPair(String name,
               String quoteTokenAddress,
               String pancakeQuoteTokenAddress,
               int decimals,
               BigDecimal defaultGraduationThreshold,
               String buyFunctionName,
               String sellFunctionName,
               boolean nativeToken) {
        this.name = name;
        this.quoteTokenAddress = quoteTokenAddress;
        this.pancakeQuoteTokenAddress = pancakeQuoteTokenAddress;
        this.decimals = decimals;
        this.defaultGraduationThreshold = defaultGraduationThreshold;
        this.buyFunctionName = buyFunctionName;
        this.sellFunctionName = sellFunctionName;
        this.nativeToken = nativeToken;
    }

    /**
     * 通用的余额查询方法，根据代币类型自动路由查询逻辑
     */
    public BigDecimal getBalance(Web3j web3j, String address) throws Exception {
        if (this.nativeToken) {
            // 原生代币 (BNB) 查询
            BigInteger balanceWei = web3j.ethGetBalance(address, DefaultBlockParameterName.LATEST).send().getBalance();
            return new BigDecimal(balanceWei).divide(BigDecimal.TEN.pow(this.decimals), 18, RoundingMode.DOWN);
        } else {
            // ERC20 代币 (USDT) 查询：通过构建 balanceOf(address) 的 Function 调用
            Function function = new Function(
                    "balanceOf",
                    Collections.singletonList(new Address(address)),
                    Collections.singletonList(new TypeReference<Uint>() {})
            );

            String encodedFunction = FunctionEncoder.encode(function);

            // 构建模拟交易（eth_call 不消耗 Gas，不需要 Credentials）
            Transaction transaction = Transaction.createEthCallTransaction(address, this.quoteTokenAddress, encodedFunction);
            EthCall response = web3j.ethCall(transaction, DefaultBlockParameterName.LATEST).send();

            if (response.hasError()) {
                throw new RuntimeException("ERC20 balanceOf 调用失败: " + response.getError().getMessage());
            }

            String value = response.getValue();
            BigInteger balanceWei = Numeric.toBigInt(value);
            return new BigDecimal(balanceWei).divide(BigDecimal.TEN.pow(this.decimals), 18, RoundingMode.DOWN);
        }
    }

    public static MarketPair fromName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return BNB;
        }
        for (MarketPair pair : values()) {
            if (pair.name.equalsIgnoreCase(name.trim())) {
                return pair;
            }
        }
        throw new IllegalArgumentException("不支持的市场交易对: " + name);
    }

    public static MarketPair fromQuoteTokenAddress(String address) {
        if (address == null || address.trim().isEmpty()) {
            return BNB;
        }
        for (MarketPair pair : values()) {
            if (pair.quoteTokenAddress.equalsIgnoreCase(address.trim())) {
                return pair;
            }
        }
        throw new IllegalArgumentException("不支持的交易对代币地址: " + address);
    }
}
