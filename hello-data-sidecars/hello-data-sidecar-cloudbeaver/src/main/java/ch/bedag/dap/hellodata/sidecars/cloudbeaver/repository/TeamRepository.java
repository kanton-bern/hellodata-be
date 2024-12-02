package ch.bedag.dap.hellodata.sidecars.cloudbeaver.repository;

import ch.bedag.dap.hellodata.sidecars.cloudbeaver.entities.cbnative.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeamRepository extends JpaRepository<Team, String> {

    List<Team> findByTeamId(String teamName);

}