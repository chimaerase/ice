package org.jbei.ice.client.login;

import org.jbei.ice.client.AbstractPresenter;
import org.jbei.ice.client.RegistryServiceAsync;
import org.jbei.ice.client.event.LoginEvent;
import org.jbei.ice.client.util.Utils;
import org.jbei.ice.shared.dto.AccountInfo;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HasWidgets;

/**
 * Presenter for the login page
 * Submits user entered login credentials to the server for validation
 * 
 * @author Hector Plahar
 */

public class LoginPresenter extends AbstractPresenter {

    private final RegistryServiceAsync service;
    private final HandlerManager eventBus;
    private final ILoginView display;
    private Mode mode;

    public LoginPresenter(RegistryServiceAsync service, HandlerManager eventBus, ILoginView display) {
        this.service = service;
        this.eventBus = eventBus;
        this.display = display;

        determineCanChangePassword();
        determineCanRegister();
        setHandler();
    }

    protected void determineCanChangePassword() {
        service.getSetting("PASSWORD_CHANGE_ALLOWED", new AsyncCallback<String>() {

            @Override
            public void onSuccess(String result) {
                if (!"yes".equalsIgnoreCase(result) && !"true".equalsIgnoreCase(result))
                    return;

                display.addForgotPasswordHandler(new ForgotPasswordHandler());
            }

            @Override
            public void onFailure(Throwable caught) {
            }
        });
    }

    protected void determineCanRegister() {
        service.getSetting("NEW_REGISTRATION_ALLOWED", new AsyncCallback<String>() {

            @Override
            public void onSuccess(String result) {
                if (!"yes".equalsIgnoreCase(result) && !"true".equalsIgnoreCase(result))
                    return;

                display.addRegisterHandler(new RegisterHandler());
            }

            @Override
            public void onFailure(Throwable caught) {
            }
        });

    }

    protected void login() {
        this.display.clearErrorMessages();
        this.display.setInputFieldsEnable(false);

        String loginName = this.display.getLoginName();
        String loginPass = this.display.getLoginPass();
        boolean error = false;

        if (loginName == null || loginName.isEmpty()) {
            this.display
                    .setLoginNameError("The username field is required and cannot be left empty!");
            error = true;
        }

        if (loginPass == null || loginPass.isEmpty()) {
            this.display
                    .setLoginPassError("The password field is required and cannot be left empty!");
            error = true;
        }

        if (error) {
            display.setInputFieldsEnable(true);
            return;
        }

        // client validation passed. attempt to login
        Utils.showWaitCursor(null);
        service.login(loginName, loginPass, new AsyncCallback<AccountInfo>() {

            @Override
            public void onSuccess(AccountInfo result) {

                if (result == null) {
                    display.setLoginPassError("The username and/or password you entered is incorrect!");
                    enableInputFields();
                    return;
                }

                eventBus.fireEvent(new LoginEvent(result, display.rememberUserOnComputer()));
                enableInputFields();
            }

            @Override
            public void onFailure(Throwable caught) {
                display.setLoginPassError("Could not retrieve authentication details.");
                enableInputFields();
            }

            private void enableInputFields() {
                display.setInputFieldsEnable(true);
                resetCursor();
            }
        });
    }

    private void resetCursor() {
        Utils.showDefaultCursor(null);
    }

    protected final void setHandler() {
        SubmitHandler handler = new SubmitHandler();

        this.display.setSubmitKeyPressHandler(handler);
        this.display.setSubmitClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                login();
            }
        });
    }

    @Override
    public void go(HasWidgets container) {
        container.clear();
        container.add(this.display.asWidget());
    }

    // inner classes

    private class ForgotPasswordHandler implements ClickHandler {

        @Override
        public void onClick(ClickEvent event) {
            mode = Mode.FORGOT_PASSWORD;
            display.switchToForgotPasswordMode();
        }
    }

    private class RegisterHandler implements ClickHandler {
        @Override
        public void onClick(ClickEvent event) {
            mode = Mode.REGISTER;
            display.switchToRegisterMode();
        }
    }

    public class SubmitHandler implements KeyPressHandler, ClickHandler {

        @Override
        public void onKeyPress(KeyPressEvent event) {
            if (event.getNativeEvent().getKeyCode() != KeyCodes.KEY_ENTER)
                return;

            login();
        }

        @Override
        public void onClick(ClickEvent event) {
            login();
        }
    }

    private enum Mode {
        FORGOT_PASSWORD, REGISTER;
    }
}
