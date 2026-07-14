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
import org.afo.llm.domain.bo.LlmProjectBo;
import org.afo.llm.domain.vo.LlmProjectVo;
import org.afo.llm.service.ILlmProjectService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * LLM 项目管理。
 *
 * @author afo
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/llm/project")
public class LlmProjectController extends BaseController {

    private final ILlmProjectService llmProjectService;

    /**
     * 查询项目列表。
     */
    @SaCheckPermission("llm:project:list")
    @GetMapping("/list")
    public TableDataInfo<LlmProjectVo> list(LlmProjectBo bo, PageQuery pageQuery) {
        return llmProjectService.queryPageList(bo, pageQuery);
    }

    /**
     * 导出项目列表。
     */
    @SaCheckPermission("llm:project:export")
    @Log(title = "LLM项目管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(LlmProjectBo bo, HttpServletResponse response) {
        List<LlmProjectVo> list = llmProjectService.queryList(bo);
        ExcelUtil.exportExcel(list, "LLM项目数据", LlmProjectVo.class, response);
    }

    /**
     * 获取项目详细信息。
     *
     * @param projectId 项目ID
     */
    @SaCheckPermission("llm:project:query")
    @GetMapping("/{projectId}")
    public R<LlmProjectVo> getInfo(@NotNull(message = "项目ID不能为空") @PathVariable Long projectId) {
        return R.ok(llmProjectService.queryById(projectId));
    }

    /**
     * 新增项目。
     */
    @SaCheckPermission("llm:project:add")
    @Log(title = "LLM项目管理", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping
    public R<Void> add(@Validated(AddGroup.class) @RequestBody LlmProjectBo bo) {
        if (!llmProjectService.checkProjectCodeUnique(bo)) {
            return R.fail("新增项目'" + bo.getProjectCode() + "'失败，项目编码已存在");
        }
        return toAjax(llmProjectService.insertByBo(bo));
    }

    /**
     * 修改项目。
     */
    @SaCheckPermission("llm:project:edit")
    @Log(title = "LLM项目管理", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody LlmProjectBo bo) {
        if (!llmProjectService.checkProjectCodeUnique(bo)) {
            return R.fail("修改项目'" + bo.getProjectCode() + "'失败，项目编码已存在");
        }
        return toAjax(llmProjectService.updateByBo(bo));
    }

    /**
     * 修改项目状态。
     */
    @SaCheckPermission("llm:project:edit")
    @Log(title = "LLM项目管理", businessType = BusinessType.UPDATE)
    @PutMapping("/changeStatus")
    public R<Void> changeStatus(@RequestBody LlmProjectBo bo) {
        return toAjax(llmProjectService.updateStatus(bo.getProjectId(), bo.getStatus()));
    }

    /**
     * 删除项目。
     *
     * @param projectIds 项目ID集合
     */
    @SaCheckPermission("llm:project:remove")
    @Log(title = "LLM项目管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{projectIds}")
    public R<Void> remove(@NotEmpty(message = "项目ID不能为空") @PathVariable Long[] projectIds) {
        return toAjax(llmProjectService.deleteWithValidByIds(List.of(projectIds), true));
    }
}
