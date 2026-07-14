package org.afo.llm.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.afo.common.core.domain.R;
import org.afo.common.core.validate.AddGroup;
import org.afo.common.core.validate.EditGroup;
import org.afo.common.idempotent.annotation.RepeatSubmit;
import org.afo.common.log.annotation.Log;
import org.afo.common.log.enums.BusinessType;
import org.afo.common.mybatis.core.page.PageQuery;
import org.afo.common.mybatis.core.page.TableDataInfo;
import org.afo.common.web.core.BaseController;
import org.afo.llm.domain.bo.LlmProviderBo;
import org.afo.llm.domain.vo.LlmProviderVo;
import org.afo.llm.service.ILlmProviderService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/llm/provider")
public class LlmProviderController extends BaseController {

    private final ILlmProviderService providerService;

    @SaCheckPermission("llm:provider:list")
    @GetMapping("/list")
    public TableDataInfo<LlmProviderVo> list(LlmProviderBo bo, PageQuery pageQuery) {
        return providerService.queryPageList(bo, pageQuery);
    }

    @GetMapping("/options")
    public R<List<Map<String, Object>>> options() {
        return R.ok(providerService.listOptions());
    }

    @GetMapping("/match")
    public R<LlmProviderVo> match(@RequestParam String modelName) {
        return R.ok(providerService.matchByModelName(modelName));
    }

    @SaCheckPermission("llm:provider:query")
    @GetMapping("/{providerId}")
    public R<LlmProviderVo> getInfo(@NotNull(message = "厂商ID不能为空") @PathVariable Long providerId) {
        return R.ok(providerService.queryById(providerId));
    }

    @SaCheckPermission("llm:provider:add")
    @Log(title = "LLM厂商管理", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping
    public R<Void> add(@Validated(AddGroup.class) @RequestBody LlmProviderBo bo) {
        return toAjax(providerService.insertByBo(bo));
    }

    @SaCheckPermission("llm:provider:edit")
    @Log(title = "LLM厂商管理", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody LlmProviderBo bo) {
        return toAjax(providerService.updateByBo(bo));
    }

    @SaCheckPermission("llm:provider:edit")
    @Log(title = "LLM厂商管理", businessType = BusinessType.UPDATE)
    @PutMapping("/changeStatus")
    public R<Void> changeStatus(@RequestBody LlmProviderBo bo) {
        return toAjax(providerService.updateStatus(bo.getProviderId(), bo.getStatus()));
    }

    @SaCheckPermission("llm:provider:remove")
    @Log(title = "LLM厂商管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{providerIds}")
    public R<Void> remove(@NotEmpty(message = "厂商ID不能为空") @PathVariable Long[] providerIds) {
        return toAjax(providerService.deleteWithValidByIds(List.of(providerIds), true));
    }
}
