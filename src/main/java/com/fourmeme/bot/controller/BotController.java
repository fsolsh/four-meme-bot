package com.fourmeme.bot.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fourmeme.bot.entity.TokenMeta;
import com.fourmeme.bot.entity.WalletTokenBalance;
import com.fourmeme.bot.response.WalletResponse;
import com.fourmeme.bot.entity.BotTask;
import com.fourmeme.bot.entity.TradeRecord;
import com.fourmeme.bot.entity.Wallet;
import com.fourmeme.bot.request.CreateTaskRequest;
import com.fourmeme.bot.request.DeriveWalletsRequest;
import com.fourmeme.bot.request.ImportWalletRequest;
import com.fourmeme.bot.request.VerifyMnemonicRequest;
import com.fourmeme.bot.response.R;
import com.fourmeme.bot.service.BotTaskService;
import com.fourmeme.bot.service.TokenMetaService;
import com.fourmeme.bot.service.TradeRecordService;
import com.fourmeme.bot.service.WalletService;
import com.fourmeme.bot.service.WalletTokenBalanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/bot")
@RequiredArgsConstructor
@Tag(name = "Volume Bot 管理", description = "钱包管理、任务调度、交易记录查询")
public class BotController {

    private final WalletService walletService;
    private final BotTaskService botTaskService;
    private final TokenMetaService tokenMetaService;
    private final TradeRecordService tradeRecordService;
    private final WalletTokenBalanceService walletTokenBalanceService;

    // ==================== 钱包管理 ====================

    @PostMapping("/wallet/create")
    @Operation(summary = "创建新钱包（仅私钥，不推荐）",
            description = "直接生成私钥，不生成助记词。私钥丢失后无法恢复，建议使用 create-with-mnemonic")
    public R<WalletResponse> createWallet() throws Exception {
        Wallet wallet = walletService.createWallet();
        return R.success(WalletResponse.from(wallet));
    }

    @PostMapping("/wallet/create-with-mnemonic")
    @Operation(summary = "创建带助记词的钱包（推荐）",
            description = "生成 12 个单词的 BIP39 助记词，私钥和助记词均加密存储")
    public R<Map<String, Object>> createWalletWithMnemonic() throws Exception {
        Wallet wallet = walletService.createWalletWithMnemonic();
        Map<String, Object> result = new HashMap<>();
        result.put("wallet", WalletResponse.from(wallet));
        result.put("warning", "请调用 /wallet/export-mnemonic/" + wallet.getId()
                + " 导出助记词并离线备份");
        return R.success(result);
    }

    @GetMapping("/wallet/{id}")
    @Operation(summary = "查询单个钱包")
    public R<WalletResponse> getWallet(
            @Parameter(description = "钱包ID", example = "1")
            @PathVariable Long id) {
        Wallet wallet = walletService.getWalletById(id);
        if (wallet == null) {
            return R.failure("钱包不存在: " + id);
        }
        long count = walletTokenBalanceService.countByWallet(id);
        return R.success(WalletResponse.from(wallet, count));
    }

    @GetMapping("/wallet/list")
    @Operation(summary = "查询所有启用的钱包")
    public R<List<WalletResponse>> listWallets() {
        List<WalletResponse> list = walletService.getAllWallets(1).stream()
                .map(w -> WalletResponse.from(w,
                        walletTokenBalanceService.countByWallet(w.getId())))
                .collect(Collectors.toList());
        return R.success(list);
    }

    @GetMapping("/wallet/export-mnemonic/{id}")
    @Operation(summary = "导出助记词（明文）",
            description = "⚠️ 生产环境建议加认证或二次验证。返回的助记词等同于钱包全部资产控制权")
    public R<Map<String, Object>> exportMnemonic(
            @Parameter(description = "钱包ID", example = "1")
            @PathVariable Long id) {
        String mnemonic = walletService.exportMnemonic(id);
        Map<String, Object> result = new HashMap<>();
        result.put("mnemonic", mnemonic);
        result.put("warning", "请立即离线备份，此助记词可恢复钱包全部资产");
        return R.success(result);
    }

    @PostMapping("/wallet/import")
    @Operation(summary = "通过助记词导入钱包",
            description = "将已有的助记词导入系统，自动派生地址和私钥并加密存储")
    public R<WalletResponse> importWallet(@RequestBody ImportWalletRequest request) throws Exception {
        if (!StringUtils.hasText(request.getMnemonic())) {
            return R.failure("助记词不能为空");
        }
        Wallet wallet = walletService.importWalletFromMnemonic(request.getMnemonic());
        return R.success(WalletResponse.from(wallet));
    }

    @PostMapping("/wallet/derive")
    @Operation(summary = "通过助记词批量派生钱包",
            description = "使用 BIP44 路径 m/44'/60'/0'/0/{index} 从一个助记词派生多个钱包地址")
    public R<Map<String, Object>> deriveWallets(@RequestBody DeriveWalletsRequest request) throws Exception {
        if (!StringUtils.hasText(request.getMnemonic())) {
            return R.failure("助记词不能为空");
        }
        if (request.getCount() == null || request.getCount() <= 0) {
            return R.failure("派生数量必须大于0");
        }
        List<Wallet> wallets = walletService.deriveWalletsFromMnemonic(
                request.getMnemonic(), request.getCount());
        Map<String, Object> result = new HashMap<>();
        result.put("count", wallets.size());
        result.put("addresses", wallets.stream()
                .map(Wallet::getAddress)
                .collect(Collectors.toList()));
        return R.success(result);
    }

    @PostMapping("/wallet/verify-mnemonic")
    @Operation(summary = "验证助记词", description = "校验助记词合法性并返回默认派生地址（index=0）")
    public R<Map<String, Object>> verifyMnemonic(@RequestBody VerifyMnemonicRequest request) {
        if (!StringUtils.hasText(request.getMnemonic())) {
            return R.failure("助记词不能为空");
        }
        String address = walletService.recoverAddressFromMnemonic(request.getMnemonic());
        Map<String, Object> result = new HashMap<>();
        result.put("address", address);
        return R.success(result);
    }

    @PostMapping("/wallet/refresh/{id}")
    @Operation(summary = "刷新单个钱包余额（含代币持仓）")
    public R<WalletResponse> refreshBalance(
            @Parameter(description = "钱包ID", example = "1")
            @PathVariable Long id) throws Exception {
        Wallet wallet = walletService.getWalletById(id);
        if (wallet == null) {
            return R.failure("钱包不存在: " + id);
        }
        walletService.refreshBalance(wallet);
        long count = walletTokenBalanceService.countByWallet(id);
        return R.success(WalletResponse.from(wallet, count));
    }

    @PostMapping("/wallet/refresh-all")
    @Operation(summary = "刷新所有钱包余额")
    public R<Void> refreshAll() {
        walletService.refreshAllBalances();
        return R.success();
    }

    @PutMapping("/wallet/{id}/enable")
    @Operation(summary = "启用钱包")
    public R<Void> enableWallet(
            @Parameter(description = "钱包ID", example = "1")
            @PathVariable Long id) {
        walletService.enableWallet(id);
        return R.success();
    }

    @PutMapping("/wallet/{id}/disable")
    @Operation(summary = "禁用钱包")
    public R<Void> disableWallet(
            @Parameter(description = "钱包ID", example = "1")
            @PathVariable Long id) {
        walletService.disableWallet(id);
        return R.success();
    }

    // ==================== 钱包资产 ====================

    @GetMapping("/wallet/{id}/tokens")
    @Operation(summary = "查询钱包的代币持仓",
            description = "返回该钱包持有的所有任务代币的数量")
    public R<List<WalletTokenBalance>> getWalletTokens(
            @Parameter(description = "钱包ID", example = "1")
            @PathVariable Long id) {
        Wallet wallet = walletService.getWalletById(id);
        if (wallet == null) {
            return R.failure("钱包不存在: " + id);
        }
        return R.success(walletTokenBalanceService.listByWallet(id));
    }

    @PostMapping("/wallet/{id}/tokens/refresh")
    @Operation(summary = "刷新钱包的代币持仓",
            description = "从链上重新拉取该钱包对所有任务代币的持仓，并返回最新列表")
    public R<List<WalletTokenBalance>> refreshWalletTokens(
            @Parameter(description = "钱包ID", example = "1")
            @PathVariable Long id) throws Exception {
        Wallet wallet = walletService.getWalletById(id);
        if (wallet == null) {
            return R.failure("钱包不存在: " + id);
        }
        walletService.refreshBalance(wallet);
        return R.success(walletTokenBalanceService.listByWallet(id));
    }

    // ==================== 任务管理 ====================

    @PostMapping("/task/create")
    @Operation(summary = "创建Volume Bot任务",
            description = "创建一个新的交易任务，默认 is_running=0，需要手动调用 start 启动")
    public R<BotTask> createTask(@RequestBody CreateTaskRequest request) {
        BotTask task = botTaskService.createTask(request);
        return R.success(task);
    }

    @GetMapping("/task/{id}")
    @Operation(summary = "查询单个任务")
    public R<BotTask> getTask(
            @Parameter(description = "任务ID", example = "1")
            @PathVariable Long id) {
        BotTask task = botTaskService.getById(id);
        if (task == null) {
            return R.failure("任务不存在: " + id);
        }
        return R.success(task);
    }

    @GetMapping("/task/list")
    @Operation(summary = "查询任务列表")
    public R<List<BotTask>> listTasks() {
        return R.success(botTaskService.listAll());
    }

    @PutMapping("/task/{id}/start")
    @Operation(summary = "启动任务")
    public R<BotTask> startTask(
            @Parameter(description = "任务ID", example = "1")
            @PathVariable Long id) {
        BotTask task = botTaskService.startTask(id);
        return R.success(task);
    }

    @PutMapping("/task/{id}/stop")
    @Operation(summary = "停止任务")
    public R<BotTask> stopTask(
            @Parameter(description = "任务ID", example = "1")
            @PathVariable Long id) {
        BotTask task = botTaskService.stopTask(id);
        return R.success(task);
    }

    @DeleteMapping("/task/{id}")
    @Operation(summary = "删除任务",
            description = "运行中的任务无法删除，需先调用 stop")
    public R<Void> deleteTask(
            @Parameter(description = "任务ID", example = "1")
            @PathVariable Long id) {
        botTaskService.deleteTask(id);
        return R.success();
    }

    // ==================== 交易记录 ====================

    @GetMapping("/trade/list")
    @Operation(summary = "分页查询交易记录")
    public R<IPage<TradeRecord>> listTrades(
            @Parameter(description = "代币简称（可选过滤）")
            @RequestParam(required = false) String tokenName,
            @Parameter(description = "钱包ID（可选过滤）")
            @RequestParam(required = false) Long walletId,
            @Parameter(description = "页码，从 1 开始")
            @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数，默认 10")
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.success(tradeRecordService.listPage(tokenName, walletId, pageNum, pageSize));
    }

    // ==================== 代币元数据 ====================

    @GetMapping("/token/info")
    @Operation(summary = "预览代币元数据", description = "从数据库或链上获取代币信息")
    public R<TokenMeta> getTokenInfo(
            @Parameter(description = "代币合约地址")
            @RequestParam String address) {
        TokenMeta metadata = tokenMetaService.getOrFetch(address);
        if (metadata == null) {
            return R.failure("无法获取代币信息");
        }
        return R.success(metadata);
    }
}