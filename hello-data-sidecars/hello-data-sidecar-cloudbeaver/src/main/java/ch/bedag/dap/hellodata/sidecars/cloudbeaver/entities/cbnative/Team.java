package ch.bedag.dap.hellodata.sidecars.cloudbeaver.entities.cbnative;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@EqualsAndHashCode
@Entity
@Table(name = "cb_team")
public class Team {
    @Id
    private String teamId;
    private String teamName;
    private String teamDescription;
    private LocalDateTime createTime;
}
