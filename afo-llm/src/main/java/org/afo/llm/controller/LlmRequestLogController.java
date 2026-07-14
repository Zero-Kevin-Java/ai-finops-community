package org.afo.llm.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.afo.common.core.domain.R;
import org.afo.common.excel.utils.ExcelUtil;
import org.afo.common.log.annotation.Log;
import org.afo.common.log.enums.BusinessType;
import org.afo.common.mybatis.core.page.PageQuery;
import org.afo.common.mybatis.core.page.TableDataInfo;
import org.afo.common.web.core.BaseController;
import org.afo.llm.domain.bo.LlmRequestLogBo;
import org.afo.llm.domain.vo.LlmRequestLogVo;
import org.afo.llm.service.ILlmRequestLogService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * LLM 请求日志管理。
 *
 * @author afo
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/llm/request-log")
public class LlmRequestLogController extends BaseController {

    private final ILlmRequestLogService llmRequestLogService;

    @SaCheckPermission("llm:requestLog:list")
    @GetMapping("/list")
    public TableDataInfo<LlmRequestLogVo> list(LlmRequestLogBo bo, PageQuery pageQuery) {
        return llmRequestLogService.queryPageList(bo, pageQuery);
    }

    @SaCheckPermission("llm:requestLog:export")
    @Log(title = "LLM请求日志管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(LlmRequestLogBo bo, HttpServletResponse response) {
        List<LlmRequestLogVo> list = llmRequestLogService.queryList(bo);
        ExcelUtil.exportExcel(list, "LLM请求日志数据", LlmRequestLogVo.class, response);
    }

    @SaCheckPermission("llm:requestLog:query")
    @GetMapping("/{requestLogId}")
    public R<LlmRequestLogVo> getInfo(@NotNull(message = "请求日志ID不能为空") @PathVariable Long requestLogId) {
        return R.ok(llmRequestLogService.queryById(requestLogId));
    }
}
