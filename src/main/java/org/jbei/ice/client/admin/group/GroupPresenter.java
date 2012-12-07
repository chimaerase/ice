package org.jbei.ice.client.admin.group;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.jbei.ice.client.ClientController;
import org.jbei.ice.client.IceAsyncCallback;
import org.jbei.ice.client.RegistryServiceAsync;
import org.jbei.ice.client.ServiceDelegate;
import org.jbei.ice.client.admin.AdminPanelPresenter;
import org.jbei.ice.client.admin.IAdminPanel;
import org.jbei.ice.client.exception.AuthenticationException;
import org.jbei.ice.shared.dto.AccountInfo;
import org.jbei.ice.shared.dto.group.GroupInfo;

import com.google.gwt.core.shared.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.user.client.rpc.AsyncCallback;

public class GroupPresenter extends AdminPanelPresenter {

    private final AdminGroupPanel view;
    private GroupInfo currentGroupSelection;

    public GroupPresenter(RegistryServiceAsync service, HandlerManager eventBus) {
        super(service, eventBus);
        this.view = new AdminGroupPanel();

        // handlers
        addGroupSelectionHandler();
        retrieveAvailableAccountsToUser();
        addGroupSaveHandler();
        addCreateGroupHandler();
        addGroupMemberDeleteDelegate();
        addGroupDeleteDelegate();
        addGroupUpdateDelegate();
        addVerifyMemberDelegate();
    }

    public void setGroups(ArrayList<GroupInfo> result) {
        view.displayGroups(result);
    }

    public void addGroupDeleteDelegate() {
        view.setDeleteGroupDelegate(new ServiceDelegate<GroupInfo>() {
            @Override
            public void execute(final GroupInfo groupInfo) {
                new IceAsyncCallback<GroupInfo>() {

                    @Override
                    protected void callService(AsyncCallback<GroupInfo> callback) throws AuthenticationException {
                        service.deleteGroup(ClientController.sessionId, groupInfo, callback);
                    }

                    @Override
                    public void onSuccess(GroupInfo result) {
                        view.removeGroup(result);
                    }
                }.go(eventBus);
            }
        });
    }

    private void addGroupUpdateDelegate() {
        view.setUpdateGroupDelegate(new ServiceDelegate<GroupInfo>() {
            @Override
            public void execute(final GroupInfo groupInfo) {
                new IceAsyncCallback<GroupInfo>() {

                    @Override
                    protected void callService(AsyncCallback<GroupInfo> callback) throws AuthenticationException {
                        service.updateGroup(ClientController.sessionId, groupInfo, callback);
                    }

                    @Override
                    public void onSuccess(GroupInfo result) {
                    }
                }.go(eventBus);
            }
        });
    }

    private void addVerifyMemberDelegate() {
        view.setVerifyRegisteredUserDelegate(new ServiceDelegate<String>() {
            @Override
            public void execute(final String email) {
                new IceAsyncCallback<AccountInfo>() {

                    @Override
                    protected void callService(AsyncCallback<AccountInfo> callback) throws AuthenticationException {
                        service.retrieveAccount(email, callback);
                    }

                    @Override
                    public void onSuccess(AccountInfo result) {
                        view.addVerifiedAccount(result);
                    }

                    @Override
                    public void onNullResult() {
                        view.addVerifiedAccount(null);
                    }
                }.go(eventBus);
            }
        });
    }

    private void addCreateGroupHandler() {
        view.setNewGroupHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                final GroupInfo info = view.getNewGroup();
                if (info == null)
                    return;

                // save new group
                new IceAsyncCallback<GroupInfo>() {

                    @Override
                    protected void callService(AsyncCallback<GroupInfo> callback) throws AuthenticationException {
                        service.createNewGroup(ClientController.sessionId, info, callback);
                    }

                    @Override
                    public void onSuccess(GroupInfo result) {
                        view.addGroupDisplay(result);
                        view.setCreateGroupVisibility(false);
                    }
                }.go(eventBus);
            }
        });
    }

    private void addGroupMemberDeleteDelegate() {
        view.setDeleteMemberDelegate(new ServiceDelegate<AccountInfo>() {
            @Override
            public void execute(final AccountInfo info) {
                new IceAsyncCallback<Boolean>() {

                    @Override
                    protected void callService(AsyncCallback<Boolean> callback) throws AuthenticationException {
                        service.removeAccountFromGroup(ClientController.sessionId, currentGroupSelection, info,
                                                       callback);
                    }

                    @Override
                    public void onSuccess(Boolean result) {
                        if (result.booleanValue()) {
                            currentGroupSelection.setMemberCount(currentGroupSelection.getMemberCount() - 1);
                            view.removeGroupMember(currentGroupSelection, info);
                        }
                        // else show error msg
                    }
                }.go(eventBus);
            }
        });
    }

    private void addGroupSaveHandler() {
        view.setGroupMemberSaveHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                final ArrayList<AccountInfo> selectedMembers = view.getSelectedMembers();
                GWT.log(selectedMembers.size() + " members selected for group " + currentGroupSelection.getLabel());
                new IceAsyncCallback<ArrayList<AccountInfo>>() {

                    @Override
                    protected void callService(AsyncCallback<ArrayList<AccountInfo>> callback)
                            throws AuthenticationException {
                        service.setGroupMembers(ClientController.sessionId, currentGroupSelection, selectedMembers,
                                                callback);
                    }

                    @Override
                    public void onSuccess(ArrayList<AccountInfo> result) {
                        Collections.sort(result, new Comparator<AccountInfo>() {
                            @Override
                            public int compare(AccountInfo o1, AccountInfo o2) {
                                return o1.getFullName().compareTo(o2.getFullName());
                            }
                        });
                        currentGroupSelection.setMembers(result);
                        view.setGroupMembers(currentGroupSelection, result);
                    }
                }.go(eventBus);
            }
        });
    }


    protected void retrieveAvailableAccountsToUser() {
        new IceAsyncCallback<ArrayList<AccountInfo>>() {

            @Override
            protected void callService(AsyncCallback<ArrayList<AccountInfo>> callback) throws AuthenticationException {
                service.retrieveAvailableAccounts(ClientController.sessionId, callback);
            }

            @Override
            public void onSuccess(ArrayList<AccountInfo> result) {
                view.setAvailableAccounts(result);
            }
        }.go(eventBus);
    }

    private void addGroupSelectionHandler() {
        this.view.setGroupSelectionHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                GroupInfo info = view.getGroupSelection(event);
                if (info == null)
                    return;

                currentGroupSelection = info;
                retrieveGroupMembers(info);
            }
        });
    }

    @Override
    public IAdminPanel getView() {
        return this.view;
    }

    protected void retrieveGroupMembers(final GroupInfo info) {
        new IceAsyncCallback<ArrayList<AccountInfo>>() {

            @Override
            protected void callService(AsyncCallback<ArrayList<AccountInfo>> callback) throws AuthenticationException {
                service.retrieveGroupMembers(ClientController.sessionId, info, callback);
            }

            @Override
            public void onSuccess(ArrayList<AccountInfo> result) {
                Collections.sort(result, new Comparator<AccountInfo>() {
                    @Override
                    public int compare(AccountInfo o1, AccountInfo o2) {
                        return o1.getFullName().compareTo(o2.getFullName());
                    }
                });

                if (!currentGroupSelection.getUuid().equalsIgnoreCase(info.getUuid()))
                    return;

                currentGroupSelection.setMembers(result);
                view.setGroupMembers(currentGroupSelection, result);
            }
        }.go(eventBus);
    }
}
