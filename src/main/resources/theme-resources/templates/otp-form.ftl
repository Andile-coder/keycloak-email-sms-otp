<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('code'); section>
    <#if section = "header">
        ${msg("enterCode")}
    <#elseif section = "form">
        <div id="kc-form">
            <div id="kc-form-wrapper">
                <form id="kc-otp-form" action="${url.loginAction}" method="post">
                    <div class="${properties.kcFormGroupClass!}">
                        <label for="code" class="${properties.kcLabelClass!}">
                            <#if channel?? && channel == "sms">
                                ${msg("smsCodeLabel")}
                            <#else>
                                ${msg("emailCodeLabel")}
                            </#if>
                        </label>
                        <input id="code" name="code" type="text" class="${properties.kcInputClass!}" 
                               autocomplete="off" required autofocus/>
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