package org.jbei.ice.lib.group;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.*;

import org.jbei.ice.lib.account.model.Account;
import org.jbei.ice.lib.dao.IDataModel;
import org.jbei.ice.lib.dto.group.GroupType;
import org.jbei.ice.lib.dto.group.UserGroup;

/**
 * Aggregate users into groups.
 *
 * @author Hector Plahar, Timothy Ham, Ziovii Dmytriv
 */
@Entity
@Table(name = "groups")
@SequenceGenerator(name = "sequence", sequenceName = "groups_id_seq", allocationSize = 1)
public class Group implements IDataModel {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "sequence")
    protected long id;

    @Column(name = "uuid", length = 36, nullable = false)
    protected String uuid;

    @Column(name = "label", length = 127, nullable = false)
    protected String label;

    @Column(name = "description", length = 255, nullable = false)
    protected String description;

    @ManyToOne
    private Account owner;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "parent")
    private Group parent;

    @Column(name = "creation_time")
    @Temporal(TemporalType.TIMESTAMP)
    private Date creationTime;

    @Column(name = "modification_time")
    @Temporal(TemporalType.TIMESTAMP)
    private Date modificationTime;

    @OneToMany(mappedBy = "parent", fetch = FetchType.EAGER, cascade = CascadeType.REMOVE)
    private Set<Group> children = new HashSet<>();

    @Enumerated(EnumType.STRING)
    private GroupType type;

    @ManyToMany(mappedBy = "groups", fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private Set<Account> members = new HashSet<>();

    // Getters and setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Group getParent() {
        return parent;
    }

    public void setParent(Group parent) {
        this.parent = parent;
    }

    public GroupType getType() {
        return type;
    }

    public void setType(GroupType type) {
        this.type = type;
    }

    public Set<Account> getMembers() {
        return members;
    }

    public Account getOwner() {
        return owner;
    }

    public void setOwner(Account owner) {
        this.owner = owner;
    }

    public Date getModificationTime() {
        return modificationTime;
    }

    public void setModificationTime(Date modificationTime) {
        this.modificationTime = modificationTime;
    }

    public Date getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(Date creationTime) {
        this.creationTime = creationTime;
    }

    public UserGroup toDataTransferObject() {
        UserGroup user = new UserGroup();
        user.setUuid(getUuid());
        user.setId(getId());
        user.setLabel(getLabel());
        user.setDescription(getDescription());
        Group parent = getParent();
        if (parent != null) {
            user.setParentId(parent.getId());
        }
        user.setType(getType());
        if (getOwner() != null)
            user.setOwnerEmail(getOwner().getEmail());
        return user;
    }
}
