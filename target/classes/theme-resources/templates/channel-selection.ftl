<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('channel'); section>
    <#if section = "header">
        ${msg("selectChannel")}
    <#elseif section = "form">
        <div id="kc-form">
            <div id="kc-form-wrapper">
                <form id="kc-channel-form" action="${url.loginAction}" method="post">
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
                    <div id="kc-form-buttons" class="${properties.kcFormGroupClass!}">
                        <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}" 
                               type="submit" value="${msg("doSubmit")}"/>
                    </div>
                </form>
            </div>
        </div>
    </#if>
</@layout.registrationLayout>