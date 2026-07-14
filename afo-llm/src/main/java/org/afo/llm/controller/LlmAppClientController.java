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
import org.afo.llm.domain.bo.LlmAppClientBo;
import org.afo.llm.domain.vo.LlmAppClientVo;
import org.afo.llm.service.ILlmAppClientService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * LLM 应用客户端管理。
 *
 * @author afo
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/llm/app-client")
public class LlmAppClientController extends BaseController {

    private final ILlmAppClientService llmAppClientService;

    /**
     * 查询应用客户端列表。
     */
    @SaCheckPermission("llm:appClient:list")
    @GetMapping("/list")
    public TableDataInfo<LlmAppClientVo> list(LlmAppClientBo bo, PageQuery pageQuery) {
        return llmAppClientService.queryPageList(bo, pageQuery);
    }

    /**
     * 导出应用客户端列表。
     */
    @SaCheckPermission("llm:appClient:export")
    @Log(title = "LLM应用客户端管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(LlmAppClientBo bo, HttpServletResponse response) {
        List<LlmAppClientVo> list = llmAppClientService.queryList(bo);
        ExcelUtil.exportExcel(list, "LLM应用客户端数据", LlmAppClientVo.class, response);
    }

    /**
     * 获取应用客户端详细信息。
     *
     * @param clientId 应用客户端ID
     */
    @SaCheckPermission("llm:appClient:query")
    @GetMapping("/{clientId}")
    public R<LlmAppClientVo> getInfo(@NotNull(message = "应用客户端ID不能为空") @PathVariable Long clientId) {
        return R.ok(llmAppClientService.queryById(clientId));
    }

    /**
     * 新增应用客户端。
     */
    @SaCheckPermission("llm:appClient:add")
    @Log(title = "LLM应用客户端管理", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping
    public R<Void> add(@Validated(AddGroup.class) @RequestBody LlmAppClientBo bo) {
        if (!llmAppClientService.checkAppCodeUnique(bo)) {
            return R.fail("新增应用客户端'" + bo.getAppCode() + "'失败，应用编码已存在");
        }
        return toAjax(llmAppClientService.insertByBo(bo));
    }

    /**
     * 修改应用客户端。
     */
    @SaCheckPermission("llm:appClient:edit")
    @Log(title = "LLM应用客户端管理", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody LlmAppClientBo bo) {
        if (!llmAppClientService.checkAppCodeUnique(bo)) {
            return R.fail("修改应用客户端'" + bo.getAppCode() + "'失败，应用编码已存在");
        }
        return toAjax(llmAppClientService.updateByBo(bo));
    }

    /**
     * 修改应用客户端状态。
     */
    @SaCheckPermission("llm:appClient:edit")
    @Log(title = "LLM应用客户端管理", businessType = BusinessType.UPDATE)
    @PutMapping("/changeStatus")
    public R<Void> changeStatus(@RequestBody LlmAppClientBo bo) {
        return toAjax(llmAppClientService.updateStatus(bo.getClientId(), bo.getStatus()));
    }

    /**
     * 删除应用客户端。
     *
     * @param clientIds 应用客户端ID集合
     */
    @SaCheckPermission("llm:appClient:remove")
    @Log(title = "LLM应用客户端管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{clientIds}")
    public R<Void> remove(@NotEmpty(message = "应用客户端ID不能为空") @PathVariable Long[] clientIds) {
        return toAjax(llmAppClientService.deleteWithValidByIds(List.of(clientIds), true));
    }
}
