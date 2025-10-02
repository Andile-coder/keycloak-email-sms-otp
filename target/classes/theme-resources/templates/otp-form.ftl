<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('code'); section>
    <#if section = "header">
        ${msg("enterCode")}
    <#elseif section = "form">
        <form id="kc-otp-form" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
            <div class="${properties.kcFormGroupClass!}">
                <div class="${properties.kcLabelWrapperClass!}">
                    <label for="code" class="${properties.kcLabelClass!}">
                        <#if channel?? && channel == "sms">
                            ${msg("smsCodeLabel")}
                        <#else>
                            ${msg("emailCodeLabel")}
                        </#if>
                    </label>
                </div>
                <div class="${properties.kcInputWrapperClass!}">
                    <input id="code" name="code" type="text" class="${properties.kcInputClass!}" 
                           autocomplete="off" required autofocus/>
                </div>
            </div>
            <div class="${properties.kcFormGroupClass!}">
                <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!}" 
                       type="submit" value="${msg("doSubmit")}"/>
            </div>
        </form>
    </#if>
</@layout.registrationLayout>