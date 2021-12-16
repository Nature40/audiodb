package audio;

import java.util.BitSet;
import java.util.concurrent.atomic.AtomicInteger;


import org.tinylog.Logger;

import util.collections.vec.Vec;

public class RoleManager {

	private Vec<Role> roles = new Vec<Role>();
	private AtomicInteger ci = new AtomicInteger(0);
	
	public final Role role_create_account;
	public final Role role_admin;
	public final Role role_readOnly;
	public final Role role_reviewedOnly;

	public RoleManager() {
		role_create_account = addRole("create_account");
		role_admin = addRole("admin", role_create_account);
		role_readOnly = addRole("readOnly");
		role_reviewedOnly = addRole("reviewedOnly");
	}

	public synchronized Role addRole(String roleName) {
		int index = ci.getAndIncrement();
		Role role = new Role(index, roleName);
		roles.add(role);
		return role;
	}

	@Deprecated
	public synchronized Role addRole(String roleName, String... roleNames) {
		int len = roleNames.length;
		Role[] containedRoles = new Role[len];
		for (int i = 0; i < len; i++) {
			Role role = getRole(roleNames[i]);
			if(role == null) {
				throw new RuntimeException("role not found");
			}
			containedRoles[i] = role;
		}
		return addRole(roleName, containedRoles);
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
			role.populate(bitSet);		
		}
		return bitSet;
	}

	public String[] getRoleNames(BitSet roleBits) {
		String[] roleNames = roleBits.stream().mapToObj(i -> this.roles.get(i).name).toArray(String[]::new);
		return roleNames;
	}

}
