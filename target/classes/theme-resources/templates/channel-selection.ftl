<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('channel'); section>
    <#if section = "header">
        ${msg("selectChannel")}
    <#elseif section = "form">
        <form id="kc-channel-form" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
            <div class="${properties.kcFormGroupClass!}">
                <div class="${properties.kcInputWrapperClass!}" style="display: flex; align-items: center; margin-bottom: 10px;">
                    <input type="radio" id="email" name="channel" value="email" checked style="margin-right: 8px;">
                    <label for="email" class="${properties.kcLabelClass!}">${msg("email")}</label>
                </div>
                <div class="${properties.kcInputWrapperClass!}" style="display: flex; align-items: center; margin-bottom: 10px;">
                    <input type="radio" id="sms" name="channel" value="sms" style="margin-right: 8px;">
                    <label for="sms" class="${properties.kcLabelClass!}">${msg("sms")}</label>
                </div>
            </div>
            <div class="${properties.kcFormGroupClass!}">
                <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!}" 
                       type="submit" value="${msg("doSubmit")}"/>
            </div>
        </form>
    </#if>
</@layout.registrationLayout>