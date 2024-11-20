package ch.bedag.dap.hellodata.sidecars.cloudbeaver.repository;

import ch.bedag.dap.hellodata.sidecars.cloudbeaver.entities.cbnative.AuthSubject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuthSubjectRepository extends JpaRepository<AuthSubject, String> {

    List<AuthSubject> findBySubjectId(String subjectId);

}