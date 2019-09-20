package audio;

import java.util.BitSet;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import util.collections.vec.Vec;

public class RoleManager {
	private static final Logger log = LogManager.getLogger();

	private Vec<Role> roles = new Vec<Role>();
	private AtomicInteger ci = new AtomicInteger(0);

	public RoleManager() {
		addRole("create_account");
		addRole("admin", "create_account");
	}

	public synchronized Role addRole(String roleName) {
		int index = ci.getAndIncrement();
		Role role = new Role(index, roleName);
		roles.add(role);
		return role;
	}

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
				log.info("role not found - add role: " + roleName);
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
