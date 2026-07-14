package org.afo.system.controller;

import cn.dev33.satoken.stp.StpUtil;
import lombok.RequiredArgsConstructor;
import org.afo.common.core.domain.R;
import org.afo.system.domain.SysMenu;
import org.afo.system.domain.vo.RouterVo;
import org.afo.system.service.ISysMenuService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * L0 简化菜单控制器
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/menu")
public class SysMenuController {

    private final ISysMenuService menuService;

    @GetMapping("/getRouters")
    public R<List<RouterVo>> getRouters() {
        Long userId = StpUtil.getLoginIdAsLong();
        List<SysMenu> menus = menuService.selectMenuTreeByUserId(userId);
        return R.ok(menuService.buildMenus(menus));
    }
}
