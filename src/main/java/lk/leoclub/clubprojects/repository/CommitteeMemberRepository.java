package lk.leoclub.clubprojects.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import lk.leoclub.clubprojects.model.CommitteeMember;

public interface CommitteeMemberRepository extends JpaRepository<CommitteeMember, String> {

    List<CommitteeMember> findAllByOrderByPositionAsc();

    boolean existsByName(String name);
}
