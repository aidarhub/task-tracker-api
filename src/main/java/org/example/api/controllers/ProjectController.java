package org.example.api.controllers;

import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.api.dto.AckDto;
import org.example.api.dto.ProjectDto;
import org.example.api.exceptions.BadRequestException;
import org.example.api.exceptions.NotFoundException;
import org.example.api.factories.ProjectDtoFactory;
import org.example.store.entities.ProjectEntity;
import org.example.store.repositories.ProjectRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true) // experimental !!!
public class ProjectController {
    ProjectRepository projectRepository;
    ProjectDtoFactory projectDtoFactory;

    public static final String FETCH_PROJECTS = "/api/projects";
    public static final String DELETE_PROJECT = "/api/projects/{task_project_id}";
    public static final String CREATE_OR_UPDATE_PROJECT = "/api/projects";

    @GetMapping(FETCH_PROJECTS)
    public List<ProjectDto> fetchProject(@RequestParam(value = "prefix_name", required = false) Optional<String> prefixName) {
        prefixName = prefixName.filter(name -> !name.trim().isEmpty());

        Stream<ProjectEntity> projectEntityStream = prefixName
                .map(projectRepository::streamAllByNameStartWithIgnoreCase)
                .orElseGet(projectRepository::streamAll);

        return projectEntityStream
                .map(projectDtoFactory::makeProjectDto)
                .collect(Collectors.toList());
    }

    @PutMapping(CREATE_OR_UPDATE_PROJECT)
    public ProjectDto createOrUpdateProject(
            @RequestParam(value = "task_project_id", required = false) Optional<Long> optionalProjectId,
            @RequestParam(value = "project_name", required = false) Optional<String> optionalProjectName) {

        optionalProjectName = optionalProjectName.filter(projectName -> !projectName.trim().isEmpty());

        if (!optionalProjectId.isPresent() && !optionalProjectName.isPresent()) {
            throw new BadRequestException("Project name can't be empty");
        }

        final ProjectEntity project = optionalProjectId
                .map(this::getProjectOrThrowException)
                .orElseGet(() -> ProjectEntity.builder().build());

        optionalProjectName.ifPresent(projectName -> {
            projectRepository
                    .findByName(projectName)
                    .filter(anotherProject -> !Objects.equals(anotherProject.getId(), project.getId()))
                    .ifPresent(anotherProject -> {
                        throw new BadRequestException(String.format("Project \"%s\" already exist", projectName));
                    });

            project.setName(projectName);
        });

        final ProjectEntity saveProject = projectRepository.saveAndFlush(project);

        return projectDtoFactory.makeProjectDto(saveProject);
    }

    @DeleteMapping(DELETE_PROJECT)
    public AckDto deleteProject(@PathVariable("task_project_id") Long projectId) {
        getProjectOrThrowException(projectId);
        projectRepository.deleteById(projectId);

        return AckDto.makeDefaulkt(true);
    }

    private ProjectEntity getProjectOrThrowException(Long projectId) {
        return projectRepository
                .findById(projectId)
                .orElseThrow(() ->
                        new NotFoundException(String.format("Project with \"%s\" doesn't exist", projectId)));
    }
}
