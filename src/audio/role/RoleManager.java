package audio.role;

import java.util.BitSet;
import java.util.concurrent.atomic.AtomicInteger;


import org.tinylog.Logger;

import util.collections.vec.Vec;

public class RoleManager {

	private Vec<Role> roles = new Vec<Role>();
	private AtomicInteger ci = new AtomicInteger(0);

	public final Role role_create_account;
	public final Role role_manage_account;
	public final Role role_admin;
	public final Role role_readOnly;
	public final Role role_reviewedOnly;

	public final RoleMask roleMask_list_account;

	public RoleManager() {
		role_create_account = addRole("create_account");
		role_manage_account = addRole("manage_account", role_create_account);
		role_admin = addRole("admin", role_manage_account);
		role_readOnly = addRole("readOnly");
		role_reviewedOnly = addRole("reviewedOnly");
		roleMask_list_account = RoleMask.ofRolesSelf(role_manage_account, role_create_account);
	}

	public synchronized Role addRole(String roleName) {
		int index = ci.getAndIncrement();
		Role role = new Role(index, roleName);
		roles.add(role);
		return role;
	}

	public synchronized Role addRole(String roleName, Role... containedRoles) {
		int index = ci.getAndIncrement();
		Role role = new Role(index, roleName, containedRoles);
		roles.add(role);
		return role;
	}

	public Role getRole(String roleName) {
		for (Role role:roles) {
			if(role.name.equals(roleName)) {
				return role;
			}
		}
		return null;
	}

	public Role getThrowRole(String roleName) {
		for (Role role:roles) {
			if(role.name.equals(roleName)) {
				return role;
			}
		}
		throw new RuntimeException("role not found: " + roleName);
	}

	public BitSet getRoleBits(String[] roleNames) {
		BitSet bitSet = new BitSet();
		for(String roleName:roleNames) {
			Role role = getRole(roleName);
			if(role == null) {
				Logger.info("role not found - add role: " + roleName);
				role = addRole(roleName);
			}
			role.populateRecursive(bitSet);		
		}
		return bitSet;
	}	

	public String[] getRoleNames(BitSet roleBits) {
		String[] roleNames = roleBits.stream().mapToObj(i -> this.roles.get(i).name).toArray(String[]::new);
		return roleNames;
	}

	public boolean isLoweringRole(Role role) {
		return role == role_readOnly || role == role_reviewedOnly;
	}

	public void checkHasRole(BitSet roleBits, String role) {
		if(role != null) {
			Role r = getRole(role);
			if(r == null) {
				throw new RuntimeException("role not found: |" + role + "|");
			}
			r.checkHas(roleBits);
		}
	}

	public boolean hasRole(BitSet roleBits, String role) {
		if(role == null) {
			return true;
		}		
		Role r = getRole(role);
		if(r == null) {
			Logger.info("role not found: |" + role + "|");
			return false;
		}
		return r.has(roleBits);
	}

	public void checkHasRoles(BitSet roleBits, String... roles) {
		if(roles != null) {
			for(String role : roles) {
				checkHasRole(roleBits, role);
			}
		}
	}

	public boolean hasRoles(BitSet roleBits, String... roles) {
		if(roles == null) {
			return true;
		}
		for(String role : roles) {
			if(!hasRole(roleBits, role)) {
				return false;
			}
		}
		return true;
	}
}
