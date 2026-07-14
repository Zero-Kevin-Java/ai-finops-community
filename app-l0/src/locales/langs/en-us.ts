const local: App.I18n.Schema = {
  system: {
    title: 'AI-FinOps',
    updateTitle: 'System Version Update Notification',
    updateContent: 'A new version of the system has been detected. Do you want to refresh the page immediately?',
    updateConfirm: 'Refresh immediately',
    updateCancel: 'Later'
  },
  common: {
    action: 'Action',
    add: 'Add',
    addSuccess: 'Add Success',
    backToHome: 'Back to home',
    batchDelete: 'Batch Delete',
    import: 'Import',
    export: 'Export',
    importSuccess: 'Import Success',
    importFail: 'Import Fail',
    importTemplate: 'Import Template',
    importResult: 'Import Result',
    downloadTemplate: 'Download Template',
    importEnd: '',
    importFormat: 'and the format is',
    importTip: 'Please upload a file no larger than',
    importSize: 'Please upload a file no larger than',
    exportSuccess: 'Export Success',
    exportFail: 'Export Fail',
    updateExisting: 'Whether to update the existing user data',
    cancel: 'Cancel',
    close: 'Close',
    check: 'Check',
    selectAll: 'Select All',
    expandColumn: 'Expand Column',
    columnSetting: 'Column Setting',
    config: 'Config',
    login: 'Login',
    confirm: 'Confirm',
    save: 'Save',
    delete: 'Delete',
    deleteSuccess: 'Delete Success',
    confirmDelete: 'Are you sure you want to delete?',
    confirmUpdateStatus: 'Are you sure you want to update status?',
    edit: 'Edit',
    download: 'Download',
    warning: 'Warning',
    error: 'Error',
    index: 'Index',
    keywordSearch: 'Please enter keyword',
    logout: 'Logout',
    logoutConfirm: 'Are you sure you want to log out?',
    lookForward: 'Coming soon',
    modify: 'Modify',
    modifySuccess: 'Modify Success',
    noData: 'No Data',
    operate: 'Operate',
    pleaseCheckValue: 'Please check whether the value is valid',
    refresh: 'Refresh',
    reset: 'Reset',
    search: 'Search',
    switch: 'Switch',
    tip: 'Tip',
    trigger: 'Trigger',
    update: 'Update',
    saveSuccess: 'Save Success',
    updateSuccess: 'Update Success',
    noChange: 'No actions were taken',
    userCenter: 'User Center',
    yesOrNo: {
      yes: 'Yes',
      no: 'No'
    },
    second: 'Second',
    selected: 'selected',
    anyRecords: 'records',
    clear: 'Clear',
    noSelectRecord: 'No Records Selected'
  },
  request: {
    logout: 'Logout user after request failed',
    logoutMsg: 'User status is invalid, please log in again',
    logoutWithModal: 'Pop up modal after request failed and then log out user',
    logoutWithModalMsg: 'User status is invalid, please log in again',
    refreshToken: 'The requested token has expired, refresh the token',
    tokenExpired: 'The requested token has expired'
  },
  theme: {
    themeDrawerTitle: 'Theme Configuration',
    tabs: {
      appearance: 'Appearance',
      layout: 'Layout',
      general: 'General',
      preset: 'Preset'
    },
    appearance: {
      themeSchema: {
        title: 'Theme Schema',
        light: 'Light',
        dark: 'Dark',
        auto: 'Follow System'
      },
      grayscale: 'Grayscale',
      colourWeakness: 'Colour Weakness',
      themeColor: {
        title: 'Theme Color',
        primary: 'Primary',
        info: 'Info',
        success: 'Success',
        warning: 'Warning',
        error: 'Error',
        followPrimary: 'Follow Primary'
      },
      themeRadius: {
        title: 'Theme Radius'
      },
      recommendColor: 'Apply Recommended Color Algorithm',
      recommendColorDesc: 'The recommended color algorithm refers to',
      preset: {
        title: 'Theme Presets',
        apply: 'Apply',
        applySuccess: 'Preset applied successfully',
        default: {
          name: 'Default Preset',
          desc: 'Default theme preset with balanced settings'
        },
        afo: {
          name: 'AI-FinOps',
          desc: 'Default theme preset of AI-FinOps'
        },
        dark: {
          name: 'Dark Preset',
          desc: 'Dark theme preset for night time usage'
        },
        compact: {
          name: 'Compact Preset',
          desc: 'Compact layout preset for small screens'
        },
        azir: {
          name: "Azir's Preset",
          desc: 'It is a cold and elegant preset that Azir likes'
        }
      }
    },
    layout: {
      layoutMode: {
        title: 'Layout Mode',
        vertical: 'Vertical Mode',
        horizontal: 'Horizontal Mode',
        'vertical-mix': 'Vertical Mix Mode',
        'vertical-hybrid-header-first': 'Left Hybrid Header-First',
        'top-hybrid-sidebar-first': 'Top-Hybrid Sidebar-First',
        'top-hybrid-header-first': 'Top-Hybrid Header-First',
        vertical_detail: 'Vertical menu layout, with the menu on the left and content on the right.',
        'vertical-mix_detail':
          'Vertical mix-menu layout, with the primary menu on the dark left side and the secondary menu on the lighter left side.',
        'vertical-hybrid-header-first_detail':
          'Left hybrid layout, with the primary menu at the top, the secondary menu on the dark left side, and the tertiary menu on the lighter left side.',
        horizontal_detail: 'Horizontal menu layout, with the menu at the top and content below.',
        'top-hybrid-sidebar-first_detail':
          'Top hybrid layout, with the primary menu on the left and the secondary menu at the top.',
        'top-hybrid-header-first_detail':
          'Top hybrid layout, with the primary menu at the top and the secondary menu on the left.'
      },
      tab: {
        title: 'Tab Settings',
        visible: 'Tab Visible',
        cache: 'Tag Bar Info Cache',
        cacheTip: 'Keep the tab bar information after leaving the page',
        height: 'Tab Height',
        mode: {
          title: 'Tab Mode',
          slider: 'Slider',
          chrome: 'Chrome',
          button: 'Button'
        },
        closeByMiddleClick: 'Close Tab by Middle Click',
        closeByMiddleClickTip: 'Enable closing tabs by clicking with the middle mouse button'
      },
      header: {
        title: 'Header Settings',
        height: 'Header Height',
        breadcrumb: {
          visible: 'Breadcrumb Visible',
          showIcon: 'Breadcrumb Icon Visible'
        }
      },
      sider: {
        title: 'Sider Settings',
        inverted: 'Dark Sider',
        width: 'Sider Width',
        collapsedWidth: 'Sider Collapsed Width',
        mixWidth: 'Mix Sider Width',
        mixCollapsedWidth: 'Mix Sider Collapse Width',
        mixChildMenuWidth: 'Mix Child Menu Width',
        autoSelectFirstMenu: 'Auto Select First Submenu',
        autoSelectFirstMenuTip:
          'When a first-level menu is clicked, the first submenu is automatically selected and navigated to the deepest level'
      },
      footer: {
        title: 'Footer Settings',
        visible: 'Footer Visible',
        fixed: 'Fixed Footer',
        height: 'Footer Height',
        right: 'Right Footer'
      },
      content: {
        title: 'Content Area Settings',
        scrollMode: {
          title: 'Scroll Mode',
          tip: 'The theme scroll only scrolls the main part, the outer scroll can carry the header and footer together',
          wrapper: 'Wrapper',
          content: 'Content'
        },
        page: {
          animate: 'Page Animate',
          mode: {
            title: 'Page Animate Mode',
            fade: 'Fade',
            'fade-slide': 'Slide',
            'fade-bottom': 'Fade Zoom',
            'fade-scale': 'Fade Scale',
            'zoom-fade': 'Zoom Fade',
            'zoom-out': 'Zoom Out',
            none: 'None'
          }
        },
        fixedHeaderAndTab: 'Fixed Header And Tab'
      }
    },
    general: {
      title: 'General Settings',
      watermark: {
        title: 'Watermark Settings',
        visible: 'Watermark Full Screen Visible',
        text: 'Custom Watermark Text',
        enableUserName: 'Enable User Name Watermark',
        enableTime: 'Show Current Time',
        timeFormat: 'Time Format'
      },
      multilingual: {
        title: 'Multilingual Settings',
        visible: 'Display multilingual button'
      },
      globalSearch: {
        title: 'Global Search Settings',
        visible: 'Display GlobalSearch button'
      }
    },
    configOperation: {
      copyConfig: 'Copy Config',
      copySuccessMsg: 'Copy Success, Please replace the variable "themeSettings" in "src/theme/settings.ts"',
      resetConfig: 'Reset Config',
      resetSuccessMsg: 'Reset Success'
    },
    tablePropsTitle: 'Table Props',
    table: {
      size: {
        title: 'Table Size',
        small: 'Small',
        medium: 'Medium',
        large: 'Large'
      },
      bordered: 'Bordered',
      bottomBordered: 'Bottom Bordered',
      singleColumn: 'Single Column',
      singleLine: 'Single Line',
      striped: 'Striped'
    }
  },
  route: {
    login: 'Login',
    403: 'No Permission',
    404: 'Page Not Found',
    500: 'Server Error',
    'iframe-page': 'Iframe',
    home: 'Home',
    'home-drilldown': 'Cost Drilldown',
    system: 'System Management',
    system_user: 'User Management',
    system_role: 'Role Management',
    system_menu: 'Menu Management',
    system_dept: 'Dept Management',
    system_post: 'Post Management',
    system_dict: 'Dict Management',
    system_config: 'Config Management',
    system_notice: 'Notice Management',
    system_oss: 'File Management',
    'system_oss-config': 'OSS Config',
    system_client: 'Client Management',
    llm: 'Management',
    llm_model: 'Model Management',
    llm_project: 'Project Management',
    'llm_app-client': 'App Management',
    'llm_api-key': 'API Key Management',
    llm_provider: 'Vendor Management',
    gateway: 'Gateway Management',
    gateway_cache: 'Gateway Cache',
    'gateway_routing-config': 'Routing Config',
    'gateway_whitelist-recommend': 'Smart Recommendations',
    system_tenant: 'Tenant Management',
    'system_tenant-package': 'Tenant Package Management',
    monitor: 'Monitor',
    monitor_logininfor: 'Login Log',
    monitor_operlog: 'Operate Log',
    monitor_cache: 'Cache Monitor',
    monitor_online: 'Online User',
    'user-center': 'User Center',
    'social-callback': 'Social Callback',
    about: 'About'
  },
  menu: {
    system_tenant: 'Tenant Management',
    system_log: 'Log Management',
    'monitor_snail-job': 'Job Management',
    monitor_admin: 'Admin Monitor'
  },
  dict: {
    sys_user_sex: {
      male: 'Male',
      female: 'Female',
      unknown: 'Unknown'
    },
    sys_show_hide: {
      show: 'Show',
      hide: 'Hide'
    },
    sys_normal_disable: {
      normal: 'Normal',
      disable: 'Disable'
    },
    sys_yes_no: {
      yes: 'Yes',
      no: 'No'
    },
    sys_notice_type: {
      notice: 'Notice',
      announcement: 'Announcement'
    },
    sys_notice_status: {
      normal: 'Normal',
      close: 'Close'
    },
    sys_oper_type: {
      insert: 'Insert',
      update: 'Update',
      delete: 'Delete',
      grant: 'Grant',
      export: 'Export',
      import: 'Import',
      force: 'Force',
      gencode: 'Generate Code',
      clean: 'Clean Data',
      other: 'Other'
    },
    sys_common_status: {
      success: 'Success',
      fail: 'Fail'
    },
    sys_grant_type: {
      password: 'Password Auth',
      sms: 'SMS Auth',
      email: 'Email Auth',
      miniapp: 'Mini App Auth',
      social: 'Social Auth'
    },
    sys_device_type: {
      pc: 'PC',
      android: 'Android',
      ios: 'iOS',
      miniapp: 'Mini App'
    },
    llm_model_type: {
      chat: 'Chat',
      embedding: 'Embedding',
      image: 'Image',
      rerank: 'Rerank',
      audio: 'Audio',
      video: 'Video'
    },
    llm_key_status: {
      normal: 'Normal',
      disabled: 'Disabled'
    },
    llm_call_source: {
      api: 'API',
      console: 'Console',
      internal: 'Internal Service'
    },
    llm_billing_unit: {
      '1k_tokens': '1K Tokens',
      '1m_tokens': '1M Tokens',
      request: 'Requests'
    },
    
    
    
    
    
    llm_app_type: {
      server: 'Server',
      web: 'Web',
      mobile: 'Mobile',
      internal: 'Internal'
    },
    
    
    
    
    wf_business_status: {
      revoked: 'Revoked',
      draft: 'Draft',
      pending: 'Pending',
      completed: 'Completed',
      cancelled: 'Cancelled',
      returned: 'Returned',
      terminated: 'Terminated'
    },
    wf_form_type: {
      custom_form: 'Custom Form',
      dynamic_form: 'Dynamic Form'
    },
    wf_task_status: {
      revoke: 'Revoke',
      pass: 'Pass',
      pending_review: 'Pending Review',
      cancel: 'Cancel',
      return: 'Return',
      terminate: 'Terminate',
      transfer: 'Transfer',
      delegate: 'Delegate',
      copy: 'Copy',
      add_sign: 'Add Sign',
      minus_sign: 'Minus Sign',
      timeout: 'Timeout'
    }
  },
  page: {
    login: {
      common: {
        title: 'Modern enterprise-level multi-tenant management system',
        subTitle: 'Provides developers with a complete enterprise management solution',
        loginOrRegister: 'Login / Register',
        register: 'Register',
        userNamePlaceholder: 'Please enter user name',
        phonePlaceholder: 'Please enter phone number',
        codePlaceholder: 'Please enter verification code',
        passwordPlaceholder: 'Please enter password',
        confirmPasswordPlaceholder: 'Please enter password again',
        codeLogin: 'Verification code login',
        confirm: 'Confirm',
        back: 'Back',
        validateSuccess: 'Verification passed',
        loginSuccess: 'Login successfully',
        welcomeBack: 'Welcome back, {userName} !'
      },
      pwdLogin: {
        title: 'Password Login',
        rememberMe: 'Remember password',
        forgetPassword: 'Forget password?',
        register: 'Register',
        otherAccountLogin: 'Other Account Login',
        otherLoginMode: 'Other Login Mode',
        superAdmin: 'Super Admin',
        admin: 'Admin',
        user: 'User'
      },
      codeLogin: {
        title: 'Verification Code Login',
        getCode: 'Get verification code',
        reGetCode: 'Reacquire after {time}s',
        sendCodeSuccess: 'Verification code sent successfully',
        imageCodePlaceholder: 'Please enter image verification code'
      },
      register: {
        title: 'Register',
        agreement: 'I have read and agree to',
        protocol: '《User Agreement》',
        policy: '《Privacy Policy》'
      },
      resetPwd: {
        title: 'Reset Password'
      },
      bindWeChat: {
        title: 'Bind WeChat'
      }
    },
    home: {
      branchDesc:
        'For the convenience of everyone in developing and updating the merge, we have streamlined the code of the main branch, only retaining the homepage menu, and the rest of the content has been moved to the example branch for maintenance. The preview address displays the content of the example branch.',
      greeting: 'Good morning, {userName}, today is another day full of vitality!',
      weatherDesc: 'Today is cloudy to clear, 20℃ - 25℃!',
      projectCount: 'Project Count',
      todo: 'Todo',
      message: 'Message',
      downloadCount: 'Download Count',
      registerCount: 'Register Count',
      schedule: 'Work and rest Schedule',
      study: 'Study',
      work: 'Work',
      rest: 'Rest',
      entertainment: 'Entertainment',
      visitCount: 'Visit Count',
      turnover: 'Turnover',
      dealCount: 'Deal Count',
      projectNews: {
        title: 'Project News',
        moreNews: 'More News',
        desc1: 'AI-FinOps (afo) completed frontend project initialization.',
        desc2: 'afo completed multi-tab and dynamic route adaptation.',
        desc3: 'AI-FinOps integrated system permissions and tenant management.',
        desc4: 'afo is improving AI cost insight and operations dashboards.',
        desc5: 'AI-FinOps continues to optimize resource, budget, and billing analysis workflows.'
      },
      creativity: 'Creativity'
    },
    common: {
      id: 'ID',
      createBy: 'Creator',
      createTime: 'Create Time',
      updateBy: 'Updater',
      updateTime: 'Update Time',
      remark: 'Remark',
      form: {
        remark: {
          required: 'Please enter remark',
          invalid: 'Remark cannot be empty'
        }
      }
    },
    llm: {
      model: {
        title: 'Model List',
        modelId: 'Model ID',
        modelCode: 'Model Name',
        displayName: 'Display Name',
        provider: 'Vendor',
        supplier: 'Supplier',
        litellmModel: 'LiteLLM Model',
        protocol: 'Protocol',
        apiKey: 'API Key',
        apiBase: 'API Base URL',
        modelType: 'Model Type',
        contextWindow: 'Context Window',
        supportsStream: 'Stream',
        supportsTool: 'Tool Calling',
        supportsVision: 'Vision',
        supportsParallelFunctionCalling: 'Parallel FC',
        supportsReasoning: 'Reasoning',
        maxInputTokens: 'Max Input Tokens',
        maxOutputTokens: 'Max Output Tokens',
        apiVersion: 'API Version',
        rateLimitTpm: 'TPM Limit',
        rateLimitRpm: 'RPM Limit',
        requestTimeout: 'Timeout(s)',
        modelTags: 'Tags',
        routingOrder: 'Routing Order',
        simpleTaskTarget: 'Simple Task Target Model',
        status: 'Status',
        remark: 'Remark',
        createTime: 'Create Time',
        statusChangeSuccess: 'Status modified successfully',
        priceConfig: 'Price Config',
        form: {
          modelId: {
            required: 'Please enter Model ID',
            invalid: 'Model ID cannot be empty'
          },
          modelCode: {
            required: 'Search or enter Model Name',
            invalid: 'Model Name cannot be empty'
          },
          displayName: {
            required: 'Please enter Display Name',
            invalid: 'Display Name cannot be empty'
          },
          provider: {
            required: 'Please enter Vendor',
            invalid: 'Vendor cannot be empty'
          },
          supplier: {
            required: 'Please enter Supplier',
            invalid: 'Supplier cannot be empty'
          },
          litellmModel: {
            required: 'Please enter LiteLLM Model',
            invalid: 'LiteLLM Model cannot be empty'
          },
          modelType: {
            required: 'Please enter Model Type',
            invalid: 'Model Type cannot be empty'
          },
          contextWindow: {
            required: 'Please enter Context Window',
            invalid: 'Context Window cannot be empty'
          },
          status: {
            required: 'Please select Status',
            invalid: 'Status cannot be empty'
          },
          remark: {
            required: 'Please enter Remark',
            invalid: 'Remark cannot be empty'
          }
        },
        addModel: 'Add Model',
        editModel: 'Edit Model',
        capabilities: 'Capabilities',
        contextLimits: 'Context Limits',
        extendedConfig: 'Extended Config',
        routingConfig: 'Routing Config (L2)'
      },
      project: {
        title: 'Project List',
        projectId: 'Project ID',
        projectCode: 'Project Code',
        projectName: 'Project Name',
        ownerUserId: 'Owner',
        status: 'Status',
        remark: 'Remark',
        createTime: 'Create Time',
        statusChangeSuccess: 'Status modified successfully',
        form: {
          projectId: {
            required: 'Please enter Project ID',
            invalid: 'Project ID cannot be empty'
          },
          projectCode: {
            required: 'Please enter Project Code',
            invalid: 'Project Code cannot be empty'
          },
          projectName: {
            required: 'Please enter Project Name',
            invalid: 'Project Name cannot be empty'
          },
          ownerUserId: {
            required: 'Please select Owner',
            invalid: 'Owner cannot be empty'
          },
          status: {
            required: 'Please select Status',
            invalid: 'Status cannot be empty'
          },
          remark: {
            required: 'Please enter Remark',
            invalid: 'Remark cannot be empty'
          }
        },
        addProject: 'Add Project',
        editProject: 'Edit Project'
      },
      provider: {
        title: 'Vendor List',
        providerId: 'Vendor ID',
        logo: 'Logo',
        providerName: 'Vendor Name',
        logoSlug: 'Logo',
        modelPrefixes: 'Model Prefixes',
        sortOrder: 'Sort',
        status: 'Status',
        remark: 'Remark',
        createTime: 'Create Time',
        statusChangeSuccess: 'Vendor status modified successfully',
        form: {
          providerId: {
            required: 'Please enter Vendor ID',
            invalid: 'Vendor ID cannot be empty'
          },
          providerName: {
            required: 'Please enter Vendor Name',
            invalid: 'Vendor Name cannot be empty'
          },
          modelPrefixes: {
            required: 'Please enter Model Prefixes',
            invalid: 'Model Prefixes cannot be empty'
          },
          status: {
            required: 'Please select Status',
            invalid: 'Status cannot be empty'
          }
        },
        addProvider: 'Add Vendor',
        editProvider: 'Edit Vendor'
      },
      
      
      appClient: {
        title: 'App Client List',
        clientId: 'Client ID',
        projectId: 'Project Name',
        appCode: 'App Code',
        appName: 'App Name',
        appType: 'App Type',
        status: 'Status',
        remark: 'Remark',
        createTime: 'Create Time',
        statusChangeSuccess: 'Status modified successfully',
        form: {
          clientId: {
            required: 'Please enter Client ID',
            invalid: 'Client ID cannot be empty'
          },
          projectId: {
            required: 'Please select Project',
            invalid: 'Project cannot be empty'
          },
          appCode: {
            required: 'Please enter App Code',
            invalid: 'App Code cannot be empty'
          },
          appName: {
            required: 'Please enter App Name',
            invalid: 'App Name cannot be empty'
          },
          appType: {
            required: 'Please select App Type',
            invalid: 'App Type cannot be empty'
          },
          status: {
            required: 'Please select Status',
            invalid: 'Status cannot be empty'
          },
          remark: {
            required: 'Please enter Remark',
            invalid: 'Remark cannot be empty'
          }
        },
        addAppClient: 'Add App Client',
        editAppClient: 'Edit App Client'
      },
      apiKey: {
        title: 'API Key Management',
        keyId: 'Key ID',
        projectId: 'Project Name',
        clientId: 'App Name',
        ownerUserId: 'Owner',
        keyName: 'Key Name',
        keyPrefix: 'API Key',
        keyScope: 'Authorized Models',
        expireTime: 'Expire Time',
        lastUsedTime: 'Last Used Time',
        neverExpire: 'Never expires',
        status: 'Status',
        remark: 'Remark',
        createTime: 'Create Time',
        statusChangeSuccess: 'Status modified successfully',
        generatedKeyTitle: 'API Key Generated',
        generatedKeyTip: 'Copy and store it now. The plaintext key cannot be viewed again after closing.',
        copyKey: 'Copy Key',
        form: {
          keyId: {
            required: 'Please enter Key ID',
            invalid: 'Key ID cannot be empty'
          },
          projectId: {
            required: 'Please select Project',
            invalid: 'Project cannot be empty'
          },
          clientId: {
            required: 'Please select App',
            invalid: 'App cannot be empty'
          },
          ownerUserId: {
            required: 'Please select Owner',
            invalid: 'Owner cannot be empty'
          },
          keyName: {
            required: 'Please enter Key Name',
            invalid: 'Key Name cannot be empty'
          },
          keyScope: {
            required: 'Please select authorized models',
            invalid: 'Authorized models cannot be empty'
          },
          expireTime: {
            never: 'Leave empty to never expire',
            required: 'Please select Expire Time',
            invalid: 'Expire Time cannot be empty'
          },
          status: {
            required: 'Please select Status',
            invalid: 'Status cannot be empty'
          },
          remark: {
            required: 'Please enter Remark',
            invalid: 'Remark cannot be empty'
          }
        },
        addApiKey: 'Add API Key',
        editApiKey: 'Edit API Key'
      }
    },
    gateway: {
      cacheConfig: {
        title: 'LLM Cache Config',
        configId: 'Config ID',
        projectId: 'Project ID',
        clientId: 'App ID',
        enabled: 'Enabled',
        matchMode: 'Match Mode',
        matchModeExact: 'Exact Match',
        matchModeSemantic: 'Semantic Similarity',
        similarityThreshold: 'Similarity Threshold',
        ttlSeconds: 'TTL (seconds)',
        maxEntries: 'Max Entries',
        totalHits: 'Hits',
        totalMisses: 'Misses',
        estimatedTokensSaved: 'Tokens Saved',
        hitRate: 'Hit Rate',
        totalEntries: 'Cache Entries',
        purge: 'Purge Cache',
        purgeConfirm: 'Confirm to purge all cache entries under this project?',
        purgeSuccess: 'Cache purged successfully',
        stats: 'Statistics',
        addConfig: 'Add Config',
        editConfig: 'Edit Config',
        scopeHint: 'App-level config takes precedence over project-level',
        enabledStatus: {
          enabled: 'Enabled',
          disabled: 'Disabled'
        },
        form: {
          projectId: { required: 'Please select a project', invalid: 'Project cannot be empty' },
          enabled: { required: 'Please select enable status', invalid: 'Enable status cannot be empty' },
          matchMode: { required: 'Please select match mode', invalid: 'Match mode cannot be empty' },
          ttlSeconds: { required: 'Please enter TTL', invalid: 'TTL cannot be empty' },
          maxEntries: { required: 'Please enter max entries', invalid: 'Max entries cannot be empty' },
          similarityThreshold: {
            required: 'Please enter similarity threshold',
            invalid: 'Similarity threshold cannot be empty'
          }
        }
      },
      cacheEntry: {
        title: 'Cache Entries',
        entryId: 'Entry ID',
        projectId: 'Project ID',
        clientId: 'App ID',
        modelCode: 'Model Code',
        promptHash: 'Prompt Hash',
        promptText: 'Prompt Content',
        responseText: 'Response Content',
        hitCount: 'Hit Count',
        lastHitAt: 'Last Hit At',
        tokenCount: 'Tokens',
        createdAt: 'Created At',
        expiresAt: 'Expires At',
        detail: 'Entry Detail',
        viewDetail: 'View Detail',
        closeDetail: 'Close'
      },
      
      
      whitelistRecommend: {
        title: 'Smart Recommendations',
        cardTitle: 'Whitelist Smart Recommendations',
        addToWhitelist: 'Add to Whitelist',
        ignore: 'Ignore',
        acceptSuccess: 'Rule added to trial-run whitelist. Observe for 3 days before enabling.',
        acceptFail: 'Operation failed, the recommendation may have already been processed',
        ignoreSuccess: 'Recommendation ignored',
        requestCount: '{count} times',
        avgConfidence: 'Avg confidence {pct}%',
        pattern: 'Recommended Regex',
        emptyTitle: 'No Recommendations',
        emptyDesc:
          'System is accumulating data. Background analysis runs daily at 2 AM on the past 7 days of shadow comparison data. First batch expected within 24 hours...',
        keyword: 'Keywords',
        sourcePath: 'Source Path',
        accepted: 'Added',
        rejected: 'Ignored',
        expired: 'Expired'
      }
    },
    system: {
      client: {
        title: 'Client List',
        clientId: 'Client ID',
        clientKey: 'Client Key',
        clientSecret: 'Client Secret',
        grantTypeList: 'Grant Type',
        deviceType: 'Device Type',
        activeTimeout: 'Token Active Timeout',
        timeout: 'Token Timeout',
        status: 'Status',
        form: {
          clientId: {
            required: 'Please enter Client ID',
            invalid: 'Client ID cannot be empty'
          },
          clientKey: {
            required: 'Please enter Client Key',
            invalid: 'Client Key cannot be empty'
          },
          clientSecret: {
            required: 'Please enter Client Secret',
            invalid: 'Client Secret cannot be empty'
          },
          grantTypeList: {
            required: 'Please select Grant Type',
            invalid: 'Grant Type cannot be empty'
          },
          deviceType: {
            required: 'Please select Device Type',
            invalid: 'Device Type cannot be empty'
          },
          activeTimeout: {
            required: 'Please enter Active Timeout',
            invalid: 'Active Timeout cannot be empty',
            tooltip: 'Specify time without operation will expire (unit: second), default 30 minutes (1800 seconds)'
          },
          timeout: {
            required: 'Please enter Timeout',
            invalid: 'Timeout cannot be empty',
            tooltip: 'Specify time will expire (unit: second), default 7 days (604800 seconds)'
          },
          status: {
            required: 'Please select Status',
            invalid: 'Status cannot be empty'
          }
        },
        addClient: 'Add Client',
        editClient: 'Edit Client'
      },
      config: {
        title: 'Config List',
        configName: 'Config Name',
        configKey: 'Config Key',
        configValue: 'Config Value',
        configType: 'Built-in',
        remark: 'Remark',
        createTime: 'Create Time',
        refreshCache: 'Refresh Cache',
        refreshCacheSuccess: 'Refresh cache successfully',
        form: {
          configId: {
            required: 'Please enter Config ID',
            invalid: 'Config ID cannot be empty'
          },
          configName: {
            required: 'Please enter Config Name',
            invalid: 'Config Name cannot be empty'
          },
          configKey: {
            required: 'Please enter Config Key',
            invalid: 'Config Key cannot be empty'
          },
          configValue: {
            required: 'Please enter Config Value',
            invalid: 'Config Value cannot be empty'
          },
          configType: {
            required: 'Please select Built-in status',
            invalid: 'Built-in status cannot be empty'
          },
          remark: {
            required: 'Please enter Remark',
            invalid: 'Remark cannot be empty'
          }
        },
        addConfig: 'Add Config',
        editConfig: 'Edit Config'
      },
      dept: {
        empty: 'No department information',
        title: 'Department List',
        parentId: 'Parent Department',
        deptName: 'Department Name',
        orderNum: 'Order Num',
        deptCategory: 'Department Category',
        leader: 'Leader',
        phone: 'Phone',
        email: 'Email',
        status: 'Status',
        sort: 'Sort',
        createTime: 'Create Time',
        expandAll: 'Expand All',
        collapseAll: 'Collapse All',
        form: {
          parentId: {
            required: 'Please select Parent Department',
            invalid: 'Parent Department cannot be empty'
          },
          deptName: {
            required: 'Please enter Department Name',
            invalid: 'Department Name cannot be empty'
          },
          orderNum: {
            required: 'Please enter Order Num',
            invalid: 'Order num cannot be empty'
          },
          deptCategory: {
            required: 'Please enter Department Category',
            invalid: 'Department category cannot be empty'
          },
          leader: {
            required: 'Please enter Leader',
            invalid: 'Leader cannot be empty'
          },
          phone: {
            required: 'Please enter Phone',
            invalid: 'Phone cannot be empty'
          },
          email: {
            required: 'Please enter Email',
            invalid: 'Email cannot be empty'
          },
          status: {
            required: 'Please select Status',
            invalid: 'Status cannot be empty'
          },
          sort: {
            required: 'Please enter Sort',
            invalid: 'Sort cannot be empty'
          },
          deptId: {
            required: 'Please enter deptId',
            invalid: 'Dept Id cannot be empty'
          }
        },
        error: {
          getDeptDataFail: 'Get dept data fail',
          getDeptUserDataFail: 'Get dept user data fail'
        },
        placeholder: {
          defaultLeaderPlaceHolder: 'Please select leader',
          addDataLeaderPlaceHolder: 'Department leader can be selected only when updating',
          deptUserIsEmptyLeaderPlaceHolder: 'Current dept has no leader'
        },
        addDept: 'Add Department',
        editDept: 'Edit Department'
      },
      dict: {
        title: 'Dictionary List',
        dictTypeTitle: 'Dictionary Type List',
        dictName: 'Dictionary Name',
        dictType: 'Dictionary Type',
        status: 'Status',
        remark: 'Remark',
        createTime: 'Create Time',
        refreshCacheSuccess: 'Refresh cache successfully',
        refreshCache: 'Refresh Cache',
        confirmDeleteDictType: 'Are you sure you want to delete dic type',
        data: {
          title: 'Dictionary Data List',
          label: 'Dictionary Label',
          value: 'Dictionary Value',
          dictSort: 'Sort',
          isDefault: 'Default',
          listClass: 'Display Style',
          cssClass: 'CSS Class',
          status: 'Status',
          remark: 'Remark',
          createTime: 'Create Time'
        },
        form: {
          dictId: {
            required: 'Please enter Dictionary Id',
            invalid: 'Dictionary Id cannot be empty'
          },
          dictCode: {
            required: 'Please enter Dictionary Code',
            invalid: 'Dictionary Code cannot be empty'
          },
          dictName: {
            required: 'Please enter Dictionary Name',
            invalid: 'Dictionary Name cannot be empty'
          },
          dictType: {
            required: 'Please enter Dictionary Type',
            invalid: 'Dictionary Type cannot be empty'
          },
          status: {
            required: 'Please select Status',
            invalid: 'Status cannot be empty'
          },
          remark: {
            required: 'Please enter Remark',
            invalid: 'Remark cannot be empty'
          },
          dictLabel: {
            required: 'Please enter Dictionary Label',
            invalid: 'Dictionary Label cannot be empty'
          },
          dictValue: {
            required: 'Please enter Dictionary Value',
            invalid: 'Dictionary Value cannot be empty'
          },
          dictSort: {
            required: 'Please enter Sort',
            invalid: 'Sort cannot be empty'
          },
          isDefault: {
            required: 'Please select Default',
            invalid: 'Default cannot be empty'
          },
          listClass: {
            required: 'Please select Display Style',
            invalid: 'Display Style cannot be empty'
          },
          cssClass: {
            required: 'Please enter CSS Class',
            invalid: 'CSS Class cannot be empty'
          }
        },
        addDict: 'Add Dictionary',
        editDict: 'Edit Dictionary',
        addDictData: 'Add Dictionary Data',
        editDictData: 'Edit Dictionary Data',
        addDictType: 'Add Dictionary Type',
        editDictType: 'Edit Dictionary Type',
        exportDictType: 'Export Dictionary Type',
        refreshDictType: 'Refresh Dictionary Type',
        dictTypeIsEmpty: 'Dictionary type is empty'
      },
      menu: {
        title: 'Menu List',
        parentId: 'Parent Menu',
        iconType: 'Icon Type',
        menuName: 'Menu Name',
        icon: 'Menu Icon',
        orderNum: 'Sort',
        perms: 'Permission Code',
        component: 'Component Path',
        path: 'Route Path',
        layout: 'Layout',
        externalPath: 'External Path',
        query: 'Route Parameters',
        iframeQuery: 'Iframe Address',
        isFrame: 'External Link',
        isCache: 'Cache',
        menuType: 'Menu Type',
        visible: 'Visible',
        status: 'Status',
        createTime: 'Create Time',
        cache: 'cache',
        noCache: 'No Cache',
        rootName: 'Root',
        buttonPermissionList: 'Button Permission List',
        emptyMenu: 'Empty Menu',
        menuDetail: 'Menu Detail',
        cascadeDeleteContent: 'Cascade delete menu will delete the selected menu and all its sub-menus, are you sure?',
        iconifyTip: 'iconify address：`https://icones.js.org`',
        isFrameTip: 'If you choose External Link, the routing address needs to start with `http(s)://`',
        isCacheTip:
          'If you select yes, it will be cached by `keep-alive`, and the `name` and address of the matching component must be consistent',
        visibleTip: 'If you choose Hide, the route will not appear in the sidebar, but it can still be accessed.',
        statusTip: 'If you choose to disable, the route will not appear in the sidebar and cannot be accessed.',
        permsTip: "Permission string defined in the controller, such as: {'@'}SaCheckPermission('system:user:list')",
        componentTip:
          'The component path to access, such as: `system/user/index`, which is in the `views` directory by default',
        pathTip:
          'Router path，Example：`user`，If the external network address needs to be accessed in the internal link,then  `http(s)://` beginning',
        layoutTip:
          'Default Layout: A layout that includes common sections such as the global header, sidebar, footer, etc;\nBlank Layout: A layout without any common sections, typically used for pages like the login page',
        form: {
          parentId: {
            required: 'Please select Parent Menu',
            invalid: 'Parent Menu cannot be empty'
          },
          menuType: {
            required: 'Please select Menu Type',
            invalid: 'Menu Type cannot be empty'
          },
          icon: {
            required: 'Please select Menu Icon',
            invalid: 'Menu Icon cannot be empty'
          },
          menuIds: {
            required: 'Please select Menu',
            invalid: 'Menu cannot be empty'
          },
          menuName: {
            required: 'Please enter Menu Name',
            invalid: 'Menu Name cannot be empty'
          },
          perms: {
            required: 'Please enter permission code',
            invalid: 'Permission code cannot be empty'
          },
          orderNum: {
            required: 'Please enter order num',
            invalid: 'Order num cannot be empty'
          },
          isFrame: {
            required: 'Please select External Link',
            invalid: 'External Link cannot be empty'
          },
          path: {
            required: 'Please enter Route Path',
            invalid: 'Route Path cannot be empty'
          },
          component: {
            required: 'Please enter Component Path',
            invalid: 'Component Path cannot be empty'
          },
          query: {
            required: 'Please enter Route Parameters',
            invalid: 'Route Parameters cannot be empty'
          },
          isCache: {
            required: 'Please select Cache',
            invalid: 'Cache cannot be empty'
          },
          visible: {
            required: 'Please select Visible',
            invalid: 'Visible cannot be empty'
          },
          status: {
            required: 'Please select Status',
            invalid: 'Status cannot be empty'
          },
          permission: {
            required: 'Please enter Permission',
            invalid: 'Permission cannot be empty'
          }
        },
        placeholder: {
          iconifyIconPlaceholder: 'Please enter an icon',
          localIconPlaceholder: 'Please select the local icon',
          queryKey: 'Please enter a key',
          queryValue: 'Please enter a value',
          queryIframe: 'Please enter a iframe address'
        },
        directory: 'Directory',
        menu: 'Menu',
        button: 'Button',
        addMenu: 'Add Menu',
        addChildMenu: 'Add Child Menu',
        editMenu: 'Edit Menu',
        cascadeDelete: 'Cascade Delete Menu'
      },
      notice: {
        title: 'Notice List',
        noticeTitle: 'Notice Title',
        noticeType: 'Notice Type',
        noticeContent: 'Notice Content',
        status: 'Status',
        createTime: 'Create Time',
        form: {
          noticeTitle: {
            required: 'Please enter Notice Title',
            invalid: 'Notice Title cannot be empty'
          },
          noticeType: {
            required: 'Please select Notice Type',
            invalid: 'Notice Type cannot be empty'
          },
          noticeContent: {
            required: 'Please enter Notice Content',
            invalid: 'Notice Content cannot be empty'
          },
          status: {
            required: 'Please select Status',
            invalid: 'Status cannot be empty'
          }
        },
        addNotice: 'Add Notice',
        editNotice: 'Edit Notice'
      },
      oss: {
        title: 'File List',
        fileName: 'File Name',
        originalName: 'Original Name',
        fileSuffix: 'File Extension',
        url: 'File URL',
        createTime: 'Create Time',
        service: 'Service Provider',
        form: {
          file: {
            required: 'Please select a file',
            invalid: 'File cannot be empty'
          }
        },
        upload: 'Upload File',
        preview: 'Preview',
        download: 'Download',
        copy: 'Copy Link',
        copySuccess: 'Copy Success'
      },
      ossConfig: {
        title: 'OSS Config List',
        configKey: 'Config Key',
        accessKey: 'Access Key',
        secretKey: 'Secret Key',
        bucketName: 'Bucket Name',
        prefix: 'Prefix',
        endpoint: 'Endpoint',
        domain: 'Custom Domain',
        isHttps: 'HTTPS',
        region: 'Region',
        status: 'Status',
        remark: 'Remark',
        createTime: 'Create Time',
        form: {
          configKey: {
            required: 'Please enter Config Key',
            invalid: 'Config Key cannot be empty'
          },
          accessKey: {
            required: 'Please enter Access Key',
            invalid: 'Access Key cannot be empty'
          },
          secretKey: {
            required: 'Please enter Secret Key',
            invalid: 'Secret Key cannot be empty'
          },
          bucketName: {
            required: 'Please enter Bucket Name',
            invalid: 'Bucket Name cannot be empty'
          },
          prefix: {
            required: 'Please enter Prefix',
            invalid: 'Prefix cannot be empty'
          },
          endpoint: {
            required: 'Please enter Endpoint',
            invalid: 'Endpoint cannot be empty'
          },
          domain: {
            required: 'Please enter Custom Domain',
            invalid: 'Custom Domain cannot be empty'
          },
          isHttps: {
            required: 'Please select HTTPS',
            invalid: 'HTTPS cannot be empty'
          },
          region: {
            required: 'Please enter Region',
            invalid: 'Region cannot be empty'
          },
          status: {
            required: 'Please select Status',
            invalid: 'Status cannot be empty'
          },
          remark: {
            required: 'Please enter Remark',
            invalid: 'Remark cannot be empty'
          }
        },
        addOssConfig: 'Add OSS Config',
        editOssConfig: 'Edit OSS Config'
      },
      post: {
        title: 'Post List',
        postCode: 'Post Code',
        postName: 'Post Name',
        postSort: 'Post Sort',
        status: 'Status',
        remark: 'Remark',
        createTime: 'Create Time',
        form: {
          postCode: {
            required: 'Please enter Post Code',
            invalid: 'Post Code cannot be empty'
          },
          postName: {
            required: 'Please enter Post Name',
            invalid: 'Post Name cannot be empty'
          },
          postSort: {
            required: 'Please enter Post Sort',
            invalid: 'Post Sort cannot be empty'
          },
          status: {
            required: 'Please select Status',
            invalid: 'Status cannot be empty'
          },
          remark: {
            required: 'Please enter Remark',
            invalid: 'Remark cannot be empty'
          }
        },
        addPost: 'Add Post',
        editPost: 'Edit Post'
      },
      role: {
        title: 'Role List',
        roleName: 'Role Name',
        roleKey: 'Role Key',
        roleSort: 'Role Sort',
        status: 'Status',
        remark: 'Remark',
        menuPermission: 'Menu Permission',
        dataScope: 'Data Scope',
        createTime: 'Create Time',
        form: {
          roleName: {
            required: 'Please enter Role Name',
            invalid: 'Role Name cannot be empty'
          },
          roleKey: {
            required: 'Please enter Role Key',
            invalid: 'Role Key cannot be empty'
          },
          roleSort: {
            required: 'Please enter Role Sort',
            invalid: 'Role Sort cannot be empty'
          },
          status: {
            required: 'Please select Status',
            invalid: 'Status cannot be empty'
          },
          remark: {
            required: 'Please enter Remark',
            invalid: 'Remark cannot be empty'
          },
          menuIds: {
            required: 'Please select Menu Permission',
            invalid: 'Menu Permission cannot be empty'
          },
          deptIds: {
            required: 'Please select Dept Permission',
            invalid: 'Dept Permission cannot be empty'
          }
        },
        addRole: 'Add Role',
        editRole: 'Edit Role',
        configPermission: 'Assign Permissions',
        authorizedUsers: 'Assign Users',
        selectMenuPermission: 'Select Menu Permission',
        selectDataScope: 'Select Data Scope',
        selectDeptPermission: 'Select Dept Permission'
      },
      tenant: {
        title: 'Tenant List',
        tenantName: 'Tenant Name',
        tenantId: 'Tenant ID',
        contactUserName: 'Contact Person',
        contactPhone: 'Contact Phone',
        companyName: 'Company Name',
        licenseNumber: 'License Number',
        address: 'Address',
        intro: 'Introduction',
        domain: 'Domain',
        packageId: 'Tenant Package',
        expireTime: 'Expiration Time',
        accountCount: 'Account Count',
        status: 'Status',
        createTime: 'Create Time',
        form: {
          tenantName: {
            required: 'Please enter Tenant Name',
            invalid: 'Tenant Name cannot be empty'
          },
          contactUserName: {
            required: 'Please enter Contact Person',
            invalid: 'Contact Person cannot be empty'
          },
          contactPhone: {
            required: 'Please enter Contact Phone',
            invalid: 'Contact Phone cannot be empty'
          },
          companyName: {
            required: 'Please enter Company Name',
            invalid: 'Company Name cannot be empty'
          },
          licenseNumber: {
            required: 'Please enter License Number',
            invalid: 'License Number cannot be empty'
          },
          address: {
            required: 'Please enter Address',
            invalid: 'Address cannot be empty'
          },
          intro: {
            required: 'Please enter Introduction',
            invalid: 'Introduction cannot be empty'
          },
          domain: {
            required: 'Please enter Domain',
            invalid: 'Domain cannot be empty'
          },
          packageId: {
            required: 'Please select Tenant Package',
            invalid: 'Tenant Package cannot be empty'
          },
          expireTime: {
            required: 'Please select Expiration Time',
            invalid: 'Expiration Time cannot be empty'
          },
          accountCount: {
            required: 'Please enter Account Count',
            invalid: 'Account Count cannot be empty'
          },
          status: {
            required: 'Please select Status',
            invalid: 'Status cannot be empty'
          }
        },
        addTenant: 'Add Tenant',
        editTenant: 'Edit Tenant'
      },
      tenantPackage: {
        title: 'Tenant Package List',
        packageName: 'Package Name',
        menuIds: 'Menu Permission',
        remark: 'Remark',
        status: 'Status',
        createTime: 'Create Time',
        form: {
          packageName: {
            required: 'Please enter Package Name',
            invalid: 'Package Name cannot be empty'
          },
          menuIds: {
            required: 'Please select Menu Permission',
            invalid: 'Menu Permission cannot be empty'
          },
          status: {
            required: 'Please select Status',
            invalid: 'Status cannot be empty'
          },
          remark: {
            required: 'Please enter Remark',
            invalid: 'Remark cannot be empty'
          }
        },
        addTenantPackage: 'Add Tenant Package',
        editTenantPackage: 'Edit Tenant Package',
        statusChangeSuccess: 'Status modified successfully'
      },
      user: {
        title: 'User List',
        userName: 'Username',
        nickName: 'Nickname',
        deptName: 'Department',
        phonenumber: 'Phone Number',
        status: 'Status',
        createTime: 'Create Time',
        password: 'Password',
        confirmPassword: 'Confirm Password',
        sex: 'Gender',
        roleIds: 'Roles',
        postIds: 'Posts',
        email: 'Email',
        avatar: 'Avatar',
        remark: 'Remark',
        form: {
          userName: {
            required: 'Please enter Username',
            invalid: 'Username cannot be empty'
          },
          nickName: {
            required: 'Please enter Nickname',
            invalid: 'Nickname cannot be empty'
          },
          deptId: {
            required: 'Please select Department',
            invalid: 'Department cannot be empty'
          },
          phonenumber: {
            required: 'Please enter Phone Number',
            invalid: 'Phone Number cannot be empty'
          },
          status: {
            required: 'Please select Status',
            invalid: 'Status cannot be empty'
          },
          password: {
            required: 'Please enter Password',
            invalid: 'Password cannot be empty'
          },
          confirmPassword: {
            required: 'Please enter Confirm Password',
            invalid: 'Confirm Password cannot be empty'
          },
          sex: {
            required: 'Please select Gender',
            invalid: 'Gender cannot be empty'
          },
          roleIds: {
            required: 'Please select Roles',
            invalid: 'Roles cannot be empty'
          },
          postIds: {
            required: 'Please select Posts',
            invalid: 'Posts cannot be empty'
          },
          email: {
            required: 'Please enter Email',
            invalid: 'Email cannot be empty'
          },
          remark: {
            required: 'Please enter Remark',
            invalid: 'Remark cannot be empty'
          }
        },
        addUser: 'Add User',
        editUser: 'Edit User',
        resetPassword: 'Reset Password',
        importUsers: 'Import Users',
        exportTemplate: 'Export Template',
        importSuccess: 'Import successful',
        statusChangeSuccess: 'Status modified successfully'
      }
    },
    about: {
      title: 'About',
      introduction: `AI-FinOps (afo) is a FinOps management platform for AI scenarios, focused on cost insight, resource operations, budget control, and financial governance for unified AI spending analysis and optimization.`,
      projectInfo: {
        title: 'Project Info',
        version: 'Version',
        latestBuildTime: 'Latest Build Time',
        documentLink: 'Document Link',
        previewLink: 'Preview Link',
        repositoryLink: 'Repository Link'
      },
      prdDep: 'Production Dependency',
      devDep: 'Development Dependency'
    }
  },
  form: {
    required: 'Cannot be empty',
    userName: {
      required: 'Please enter user name',
      invalid: 'User name format is incorrect'
    },
    phone: {
      required: 'Please enter phone number',
      invalid: 'Phone number format is incorrect'
    },
    pwd: {
      required: 'Please enter password',
      invalid: '6-18 characters, including letters, numbers, and underscores'
    },
    confirmPwd: {
      required: 'Please enter password again',
      invalid: 'The two passwords are inconsistent'
    },
    code: {
      required: 'Please enter verification code',
      invalid: 'Verification code format is incorrect'
    },
    email: {
      required: 'Please enter email',
      invalid: 'Email format is incorrect'
    }
  },
  dropdown: {
    closeCurrent: 'Close Current',
    closeOther: 'Close Other',
    closeLeft: 'Close Left',
    closeRight: 'Close Right',
    closeAll: 'Close All',
    pin: 'Pin Tab',
    unpin: 'Unpin Tab'
  },
  icon: {
    themeConfig: 'Theme Configuration',
    themeSchema: 'Theme Schema',
    lang: 'Switch Language',
    fullscreen: 'Fullscreen',
    fullscreenExit: 'Exit Fullscreen',
    reload: 'Reload Page',
    collapse: 'Collapse Menu',
    expand: 'Expand Menu',
    pin: 'Pin',
    unpin: 'Unpin'
  },
  datatable: {
    itemCount: 'Total {total} items',
    fixed: {
      left: 'Left Fixed',
      right: 'Right Fixed',
      unFixed: 'Unfixed'
    }
  }
};

export default local;
