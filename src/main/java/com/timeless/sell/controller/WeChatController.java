package com.timeless.sell.controller;

import com.timeless.sell.config.WeChatAccountConfig;
import com.timeless.sell.enums.ResultEnum;
import com.timeless.sell.exception.SellException;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.api.WxConsts;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.result.WxMpOAuth2AccessToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.net.URLEncoder;

/**
 * @author lijiayin
 */
@Slf4j
@Controller
@RequestMapping("/wechat")
public class WeChatController {
    
    @Autowired
    private WxMpService wxMpService;
    
    @Autowired
    private WxMpService wxOpenService;
    
    @Autowired
    private WeChatAccountConfig accountConfig;
    
    @GetMapping("/authorize")
    public String authorize(@RequestParam("returnUrl") String returnUrl){
        //1.配置
        //2.调用方法
        String redirectUrl = wxMpService.oauth2buildAuthorizationUrl(accountConfig.getWeChatMpAuthorize() + "/sell/wechat/userInfo", WxConsts.OAuth2Scope.SNSAPI_BASE, URLEncoder.encode(returnUrl));
        log.info("【微信网页授权】获取code，redirectUrl={}", redirectUrl);
        return "redirect:" + redirectUrl;
    }
    
    @GetMapping("/userInfo")
    public String userInfo(@RequestParam("code") String code,
                         @RequestParam("state") String returnUrl){
        WxMpOAuth2AccessToken wxMpOAuth2AccessToken;
        try {
            wxMpOAuth2AccessToken = 
                    wxMpService.oauth2getAccessToken(code);
        } catch (WxErrorException e) {
            log.error("【微信网页授权】{}", e.getMessage(), e);
            throw new SellException(ResultEnum.WECHAT_MP_ERROR.getCode(), e.getError().getErrorMsg());
        }
        String openId = wxMpOAuth2AccessToken.getOpenId();
        
        log.info("openid={}", openId);
        
        return "redirect:" + returnUrl + "?openid=" + openId;
    }

    @GetMapping("/qrAuthorize")
    public String qrAuthorize(@RequestParam("returnUrl") String returnUrl){
        //1.配置
        //2.调用方法
        String redirectUrl = wxOpenService.buildQrConnectUrl(accountConfig.getWeChatOpenAuthorize() + "/sell/wechat/qrUserInfo", WxConsts.QrConnectScope.SNSAPI_LOGIN, URLEncoder.encode(returnUrl));
        log.info("【微信网页授权】获取code，redirectUrl={}", redirectUrl);
        return "redirect:" + redirectUrl;
    }

    @GetMapping("/qrUserInfo")
    public String qrUserInfo(@RequestParam("code") String code,
                           @RequestParam("state") String returnUrl){
        WxMpOAuth2AccessToken wxMpOAuth2AccessToken;
        try {
            wxMpOAuth2AccessToken =
                    wxOpenService.oauth2getAccessToken(code);
        } catch (WxErrorException e) {
            log.error("【微信网页授权】{}", e.getMessage(), e);
            throw new SellException(ResultEnum.WECHAT_MP_ERROR.getCode(), e.getError().getErrorMsg());
        }
        String openId = wxMpOAuth2AccessToken.getOpenId();

        log.info("openid={}", openId);

        return "redirect:" + returnUrl + "?openid=" + openId;
    }
}
