package org.afo.llm.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.afo.common.core.domain.R;
import org.afo.common.core.validate.AddGroup;
import org.afo.common.core.validate.EditGroup;
import org.afo.common.excel.utils.ExcelUtil;
import org.afo.common.idempotent.annotation.RepeatSubmit;
import org.afo.common.log.annotation.Log;
import org.afo.common.log.enums.BusinessType;
import org.afo.common.mybatis.core.page.PageQuery;
import org.afo.common.mybatis.core.page.TableDataInfo;
import org.afo.common.web.core.BaseController;
import org.afo.llm.domain.bo.LlmApiKeyBo;
import org.afo.llm.domain.vo.LlmApiKeyVo;
import org.afo.llm.service.ILlmApiKeyService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * LLM 业务 API Key 管理。
 *
 * @author afo
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/llm/api-key")
public class LlmApiKeyController extends BaseController {

    private final ILlmApiKeyService llmApiKeyService;

    /**
     * 查询 API Key 列表。
     */
    @SaCheckPermission("llm:apiKey:list")
    @GetMapping("/list")
    public TableDataInfo<LlmApiKeyVo> list(LlmApiKeyBo bo, PageQuery pageQuery) {
        return llmApiKeyService.queryPageList(bo, pageQuery);
    }

    /**
     * 导出 API Key 列表。
     */
    @SaCheckPermission("llm:apiKey:export")
    @Log(title = "LLM API Key管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(LlmApiKeyBo bo, HttpServletResponse response) {
        List<LlmApiKeyVo> list = llmApiKeyService.queryList(bo);
        ExcelUtil.exportExcel(list, "LLM API Key数据", LlmApiKeyVo.class, response);
    }

    /**
     * 获取 API Key 详细信息。
     *
     * @param keyId API Key ID
     */
    @SaCheckPermission("llm:apiKey:query")
    @GetMapping("/{keyId}")
    public R<LlmApiKeyVo> getInfo(@NotNull(message = "API Key ID不能为空") @PathVariable Long keyId) {
        return R.ok(llmApiKeyService.queryById(keyId));
    }

    /**
     * 新增 API Key，仅返回一次明文 Key。
     */
    @SaCheckPermission("llm:apiKey:add")
    @Log(title = "LLM API Key管理", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping
    public R<String> add(@Validated(AddGroup.class) @RequestBody LlmApiKeyBo bo) {
        String plainKey = llmApiKeyService.insertByBo(bo);
        return R.ok("操作成功", plainKey);
    }

    /**
     * 修改 API Key。
     */
    @SaCheckPermission("llm:apiKey:edit")
    @Log(title = "LLM API Key管理", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody LlmApiKeyBo bo) {
        return toAjax(llmApiKeyService.updateByBo(bo));
    }

    /**
     * 修改 API Key 状态。
     */
    @SaCheckPermission("llm:apiKey:edit")
    @Log(title = "LLM API Key管理", businessType = BusinessType.UPDATE)
    @PutMapping("/changeStatus")
    public R<Void> changeStatus(@RequestBody LlmApiKeyBo bo) {
        return toAjax(llmApiKeyService.updateStatus(bo.getKeyId(), bo.getStatus()));
    }

    /**
     * 删除 API Key。
     *
     * @param keyIds API Key ID集合
     */
    @SaCheckPermission("llm:apiKey:remove")
    @Log(title = "LLM API Key管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{keyIds}")
    public R<Void> remove(@NotEmpty(message = "API Key ID不能为空") @PathVariable Long[] keyIds) {
        return toAjax(llmApiKeyService.deleteWithValidByIds(List.of(keyIds), true));
    }
}
