package org.jbei.ice.lib.models;

import java.io.Serializable;

public class AccountEntryRelationship implements Serializable {
	private int id;
	private Account account;
	private Entry entry;
	
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public Account getAccount() {
		return account;
	}
	public void setAccount(Account account) {
		this.account = account;
	}
	public Entry getEntry() {
		return entry;
	}
	public void setEntry(Entry entry) {
		this.entry = entry;
	}
	 
}