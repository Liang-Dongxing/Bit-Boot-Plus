package com.bit.web.service.impl;

import cn.dev33.satoken.stp.SaLoginModel;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.bit.common.core.constant.Constants;
import com.bit.common.core.constant.GlobalConstants;
import com.bit.common.core.domain.model.LoginBody;
import com.bit.common.core.domain.model.LoginUser;
import com.bit.common.core.enums.LoginType;
import com.bit.common.core.enums.UserStatus;
import com.bit.common.core.exception.user.CaptchaExpireException;
import com.bit.common.core.exception.user.UserException;
import com.bit.common.core.utils.MessageUtils;
import com.bit.common.core.utils.StringUtils;
import com.bit.common.core.utils.ValidatorUtils;
import com.bit.common.core.validate.auth.SmsGroup;
import com.bit.common.redis.utils.RedisUtils;
import com.bit.common.satoken.utils.LoginHelper;
import com.bit.common.tenant.helper.TenantHelper;
import com.bit.system.domain.SysClient;
import com.bit.system.domain.SysUser;
import com.bit.system.domain.vo.SysUserVo;
import com.bit.system.mapper.SysUserMapper;
import com.bit.web.domain.vo.LoginVo;
import com.bit.web.service.IAuthStrategy;
import com.bit.web.service.SysLoginService;
import org.springframework.stereotype.Service;

/**
 * 短信认证策略
 *
 * @author Michelle.Chung
 */
@Slf4j
@Service("sms" + IAuthStrategy.BASE_NAME)
@RequiredArgsConstructor
public class SmsAuthStrategy implements IAuthStrategy {

    private final SysLoginService loginService;
    private final SysUserMapper userMapper;

    @Override
    public void validate(LoginBody loginBody) {
        ValidatorUtils.validate(loginBody, SmsGroup.class);
    }

    @Override
    public LoginVo login(String clientId, LoginBody loginBody, SysClient client) {
        String tenantId = loginBody.getTenantId();
        String phonenumber = loginBody.getPhonenumber();
        String smsCode = loginBody.getSmsCode();

        // 通过手机号查找用户
        SysUserVo user = loadUserByPhonenumber(tenantId, phonenumber);

        loginService.checkLogin(LoginType.SMS, tenantId, user.getUserName(), () -> !validateSmsCode(tenantId, phonenumber, smsCode));
        // 此处可根据登录用户的数据不同 自行创建 loginUser 属性不够用继承扩展就行了
        LoginUser loginUser = loginService.buildLoginUser(user);
        SaLoginModel model = new SaLoginModel();
        model.setDevice(client.getDeviceType());
        // 自定义分配 不同用户体系 不同 token 授权时间 不设置默认走全局 yml 配置
        // 例如: 后台用户30分钟过期 app用户1天过期
        model.setTimeout(client.getTimeout());
        model.setActiveTimeout(client.getActiveTimeout());
        // 生成token
        LoginHelper.login(loginUser, model);

        loginService.recordLogininfor(loginUser.getTenantId(), user.getUserName(), Constants.LOGIN_SUCCESS, MessageUtils.message("user.login.success"));
        loginService.recordLoginInfo(user.getUserId());
        LoginVo loginVo = new LoginVo();
        loginVo.setAccessToken(StpUtil.getTokenValue());
        return loginVo;
    }

    /**
     * 校验短信验证码
     */
    private boolean validateSmsCode(String tenantId, String phonenumber, String smsCode) {
        String code = RedisUtils.getCacheObject(GlobalConstants.CAPTCHA_CODE_KEY + phonenumber);
        if (StringUtils.isBlank(code)) {
            loginService.recordLogininfor(tenantId, phonenumber, Constants.LOGIN_FAIL, MessageUtils.message("user.jcaptcha.expire"));
            throw new CaptchaExpireException();
        }
        return code.equals(smsCode);
    }

    private SysUserVo loadUserByPhonenumber(String tenantId, String phonenumber) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
            .select(SysUser::getPhonenumber, SysUser::getStatus)
            .eq(TenantHelper.isEnable(), SysUser::getTenantId, tenantId)
            .eq(SysUser::getPhonenumber, phonenumber));
        if (ObjectUtil.isNull(user)) {
            log.info("登录用户：{} 不存在.", phonenumber);
            throw new UserException("user.not.exists", phonenumber);
        } else if (UserStatus.DISABLE.getCode().equals(user.getStatus())) {
            log.info("登录用户：{} 已被停用.", phonenumber);
            throw new UserException("user.blocked", phonenumber);
        }
        if (TenantHelper.isEnable()) {
            return userMapper.selectTenantUserByPhonenumber(phonenumber, tenantId);
        }
        return userMapper.selectUserByPhonenumber(phonenumber);
    }

}
