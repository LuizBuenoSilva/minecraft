package br.com.amigossmp.manager;

import java.util.*;

public class PartyManager {
    private final Map<UUID, Party> partiesByMember = new HashMap<>();
    private final Map<UUID, UUID> invites = new HashMap<>();
    public Party create(UUID leader) {
        Party existing = partiesByMember.get(leader);
        if (existing != null) return existing;
        Party party = new Party(leader);
        partiesByMember.put(leader, party);
        return party;
    }
    public Party get(UUID member) { return partiesByMember.get(member); }
    public void invite(UUID leader, UUID target) { invites.put(target, leader); }
    public boolean accept(UUID target) {
        UUID leader = invites.remove(target);
        if (leader == null) return false;
        Party party = partiesByMember.get(leader);
        if (party == null) return false;
        party.members.add(target);
        partiesByMember.put(target, party);
        return true;
    }
    public void leave(UUID member) {
        Party party = partiesByMember.get(member);
        if (party == null) return;
        party.members.remove(member);
        partiesByMember.remove(member);
        if (party.leader.equals(member)) {
            for (UUID id : new ArrayList<>(party.members)) partiesByMember.remove(id);
            party.members.clear();
        }
    }
    public static class Party {
        private final UUID leader;
        private final Set<UUID> members = new LinkedHashSet<>();
        public Party(UUID leader) { this.leader = leader; this.members.add(leader); }
        public UUID leader() { return leader; }
        public Set<UUID> members() { return Collections.unmodifiableSet(members); }
    }
}
