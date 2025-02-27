package ch.bedag.dap.hellodata.sidecars.cloudbeaver.entities.cbnative;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode
@Entity
@Table(name = "cb_auth_subject")
public class AuthSubject {
    @Id
    private String subjectId;
    private String subjectType;
    private String isSecretStorage;
}