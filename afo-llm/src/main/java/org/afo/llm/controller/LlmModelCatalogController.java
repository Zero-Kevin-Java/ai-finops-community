package org.afo.llm.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.afo.common.core.domain.R;
import org.afo.common.core.utils.StringUtils;
import org.afo.common.core.utils.crypto.AesEncryptor;
import org.afo.common.core.validate.AddGroup;
import org.afo.common.core.validate.EditGroup;
import org.afo.common.excel.utils.ExcelUtil;
import org.afo.common.idempotent.annotation.RepeatSubmit;
import org.afo.common.log.annotation.Log;
import org.afo.common.log.enums.BusinessType;
import org.afo.common.mybatis.core.page.PageQuery;
import org.afo.common.mybatis.core.page.TableDataInfo;
import org.afo.common.web.core.BaseController;
import org.afo.llm.domain.LlmModelCatalog;
import org.afo.llm.domain.bo.LlmModelCatalogBo;
import org.afo.llm.domain.vo.LlmModelCatalogVo;
import org.afo.llm.domain.vo.ModelConfigVo;
import org.afo.llm.service.ILlmModelCatalogService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * LLM 模型目录管理。
 *
 * @author afo
 */
@Validated
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/llm/model")
public class LlmModelCatalogController extends BaseController {

    private final ILlmModelCatalogService llmModelCatalogService;
    private final AesEncryptor aesEncryptor;

    @Value("${afo.gateway.admin.internal-token:}")
    private String internalToken;

    /**
     * 查询模型目录列表。
     */
    @SaCheckPermission("llm:model:list")
    @GetMapping("/list")
    public TableDataInfo<LlmModelCatalogVo> list(LlmModelCatalogBo bo, PageQuery pageQuery) {
        return llmModelCatalogService.queryPageList(bo, pageQuery);
    }

    /**
     * 导出模型目录列表。
     */
    @SaCheckPermission("llm:model:export")
    @Log(title = "LLM模型管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(LlmModelCatalogBo bo, HttpServletResponse response) {
        List<LlmModelCatalogVo> list = llmModelCatalogService.queryList(bo);
        ExcelUtil.exportExcel(list, "LLM模型数据", LlmModelCatalogVo.class, response);
    }

    /**
     * 获取模型目录详细信息。
     *
     * @param modelId 模型ID
     */
    @SaCheckPermission("llm:model:query")
    @GetMapping("/{modelId}")
    public R<LlmModelCatalogVo> getInfo(@NotNull(message = "模型ID不能为空") @PathVariable Long modelId) {
        return R.ok(llmModelCatalogService.queryById(modelId));
    }

    /**
     * 新增模型目录。
     */
    @SaCheckPermission("llm:model:add")
    @Log(title = "LLM模型管理", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping
    public R<Void> add(@Validated(AddGroup.class) @RequestBody LlmModelCatalogBo bo) {
        if (!llmModelCatalogService.checkModelCodeUnique(bo)) {
            return R.fail("新增模型'" + bo.getModelCode() + "'失败，模型编码已存在");
        }
        return toAjax(llmModelCatalogService.insertByBo(bo));
    }

    /**
     * 修改模型目录。
     */
    @SaCheckPermission("llm:model:edit")
    @Log(title = "LLM模型管理", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody LlmModelCatalogBo bo) {
        if (!llmModelCatalogService.checkModelCodeUnique(bo)) {
            return R.fail("修改模型'" + bo.getModelCode() + "'失败，模型编码已存在");
        }
        return toAjax(llmModelCatalogService.updateByBo(bo));
    }

    /**
     * 修改模型状态。
     */
    @SaCheckPermission("llm:model:edit")
    @Log(title = "LLM模型管理", businessType = BusinessType.UPDATE)
    @PutMapping("/changeStatus")
    public R<Void> changeStatus(@RequestBody LlmModelCatalogBo bo) {
        return toAjax(llmModelCatalogService.updateStatus(bo.getModelId(), bo.getStatus()));
    }

    /**
     * 删除模型目录。
     *
     * @param modelIds 模型ID集合
     */
    @SaCheckPermission("llm:model:remove")
    @Log(title = "LLM模型管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{modelIds}")
    public R<Void> remove(@NotEmpty(message = "模型ID不能为空") @PathVariable Long[] modelIds) {
        return toAjax(llmModelCatalogService.deleteWithValidByIds(List.of(modelIds), true));
    }

    /**
     * 获取模型下拉选项（用于价格管理、策略管理等表单）。
     */
    @GetMapping("/options")
    public R<List<Map<String, Object>>> options() {
        return R.ok(llmModelCatalogService.listOptions());
    }

    @GetMapping("/config/{tenantId}/{modelCode}")
    public R<ModelConfigVo> getModelConfig(
            @RequestHeader(value = "X-Internal-Token", required = false) String token,
            @PathVariable String tenantId,
            @PathVariable String modelCode,
            HttpServletResponse response) {
        return getModelConfigByQuery(token, tenantId, modelCode, response);
    }

    @GetMapping("/config")
    public R<ModelConfigVo> getModelConfigByQuery(
            @RequestHeader(value = "X-Internal-Token", required = false) String token,
            @RequestParam String tenantId,
            @RequestParam String modelCode,
            HttpServletResponse response) {
        if (StringUtils.isBlank(internalToken)
            || !internalToken.equals(token)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return R.fail(403, "Forbidden");
        }
        LlmModelCatalog model = llmModelCatalogService.getModelConfig(tenantId, modelCode);
        if (model == null) {
            return R.fail("Model config not found");
        }
        ModelConfigVo vo = new ModelConfigVo();
        vo.setLitellmModel(model.getLitellmModel());
        String decryptedApiKey = null;
        if (StringUtils.isNotBlank(model.getApiKey())) {
            try {
                decryptedApiKey = aesEncryptor.decrypt(model.getApiKey());
            } catch (Exception e) {
                log.error("Failed to decrypt api_key for model {}", modelCode, e);
            }
        }
        vo.setApiKey(decryptedApiKey);
        vo.setApiBase(model.getApiBase());
        vo.setProtocol(model.getProtocol());
        return R.ok(vo);
    }
}
