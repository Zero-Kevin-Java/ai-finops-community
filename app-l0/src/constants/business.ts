import { transformRecordToOption } from '@/utils/common';

/** enable status */
export const enableStatusRecord: Record<Api.Common.EnableStatus, string> = {
  '0': '正常',
  '1': '停用'
};

export const enableStatusOptions = transformRecordToOption(enableStatusRecord);

/** yes or no status */
export const yesOrNoStatusRecord: Record<Api.Common.YesOrNoStatus, string> = {
  Y: '是',
  N: '否'
};

export const yesOrNoStatusOptions = transformRecordToOption(yesOrNoStatusRecord);

/** menu type */
export const menuTypeRecord: Record<Api.System.MenuType, string> = {
  M: '目录',
  C: '菜单',
  F: '按钮'
};

export const menuTypeOptions = transformRecordToOption(menuTypeRecord);

/** menu is frame */
export const menuIsFrameRecord: Record<Api.System.IsMenuFrame, string> = {
  '0': '是',
  '1': '否',
  '2': 'iframe'
};

export const menuIsFrameOptions = transformRecordToOption(menuIsFrameRecord);

/** menu icon type */
export const menuIconTypeRecord: Record<Api.System.IconType, string> = {
  '1': 'iconify',
  '2': '本地图标'
};

export const menuIconTypeOptions = transformRecordToOption(menuIconTypeRecord);

/** menu layout */
export const menuLayoutRecord: Record<Api.System.MenuLayout, string> = {
  '0': '默认布局',
  '1': '空白布局'
};

export const menuLayoutOptions = transformRecordToOption(menuLayoutRecord);

// gen* contants removed — tool/gen deleted (L1 dev-tool)

/** oss config is https */
export const ossConfigIsHttpsRecord: Record<Api.Common.YesOrNoStatus, string> = {
  Y: 'https://',
  N: 'http://'
};

export const ossConfigIsHttpsOptions = transformRecordToOption(ossConfigIsHttpsRecord);

/** oss access policy */
export const ossAccessPolicyRecord: Record<Api.System.OssAccessPolicy, string> = {
  '0': '私有',
  '1': '公有',
  '2': '自定义'
};

export const ossAccessPolicyOptions = transformRecordToOption(ossAccessPolicyRecord);

/** data scope */
export const dataScopeRecord: Record<Api.System.DataScope, string> = {
  '1': '全部数据权限',
  '2': '自定数据权限',
  '3': '本部门数据权限',
  '4': '本部门及以下数据权限',
  '5': '仅本人数据权限',
  '6': '部门及以下或本人数据权限'
};

export const dataScopeOptions = transformRecordToOption(dataScopeRecord);
