package ch.bedag.dap.hellodata.jupyterhub.sidecar.controller;


import ch.bedag.dap.hellodata.jupyterhub.sidecar.service.TemporaryUserService;
import ch.bedag.dap.hellodata.jupyterhub.sidecar.service.dto.TemporaryUserResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
public class TemporaryUserController {

    private final TemporaryUserService temporaryUserService;

    @GetMapping("/create-temporary-user")
    public TemporaryUserResponseDto createTemporaryUser() {
        return temporaryUserService.createTemporaryUser();
    }
}
