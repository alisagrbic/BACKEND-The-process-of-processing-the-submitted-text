package com.example.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.camunda.bpm.engine.FormService;
import org.camunda.bpm.engine.IdentityService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.form.FormField;
import org.camunda.bpm.engine.form.TaskFormData;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.DTO.FormFieldsDto;
import com.example.DTO.TaskDto;
import com.example.DTO.SciFieldDto;
import model.Scientificfield;
import com.example.DTO.FieldDto;
import com.example.services.ScientificFieldsService;

@RestController
@RequestMapping("/registration")
public class RegistrationController {

	@Autowired
	IdentityService identityService;
	
	@Autowired
	private RuntimeService runtimeService;
	
	@Autowired
	TaskService taskService;  
	
	@Autowired
	FormService formService;
	
	@Autowired
	ScientificFieldsService scientificService;
	
	
	@GetMapping(path = "/get", produces = "application/json")
    public @ResponseBody FormFieldsDto get() {
		//provera da li korisnik sa id-jem pera postoji;
		//List<User> users = identityService.createUserQuery().userId("pera").list();
		ProcessInstance pi = runtimeService.startProcessInstanceByKey("Process_Reg");

		Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).list().get(0);
		
		TaskFormData tfd = formService.getTaskFormData(task.getId());
		List<FormField> properties = tfd.getFormFields();
		for(FormField fp : properties) {
			System.out.println(fp.getId() + fp.getType());
		}
		
        return new FormFieldsDto(task.getId(), pi.getId(), properties);
    }
	
	@GetMapping(path = "/get/tasks/{processInstanceId}", produces = "application/json")
    public @ResponseBody ResponseEntity<List<TaskFormData>> get(@PathVariable String processInstanceId) {
		
		List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstanceId).list();
		List<TaskDto> dtos = new ArrayList<TaskDto>();
		List<FormField> properties = new ArrayList<FormField>();
		for (Task task : tasks) {
			TaskDto t = new TaskDto(task.getId(), task.getName(), task.getAssignee());
			dtos.add(t);
			
			//uzmi formu
			TaskFormData tfd = formService.getTaskFormData(task.getId());
			 properties = tfd.getFormFields();
		}	
		
		//FormSubmissionDto formReg = (FormSubmissionDto) runtimeService.getVariable(processInstanceId, "registration");
				
        return new ResponseEntity(properties, HttpStatus.OK);
    }
	
	@GetMapping(path = "/get/firstTask/{processInstanceId}", produces = "application/json")
    public @ResponseBody ResponseEntity<List<TaskDto>> getTask(@PathVariable String processInstanceId) {
		
		List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstanceId).list();
		List<TaskDto> dtos = new ArrayList<TaskDto>();
		List<FormField> properties = new ArrayList<FormField>();
		for (Task task : tasks) {
			TaskDto t = new TaskDto(task.getId(), task.getName(), task.getAssignee());
			dtos.add(t);
		}	
		//FormSubmissionDto formReg = (FormSubmissionDto) runtimeService.getVariable(processInstanceId, "registration");
				
        return new ResponseEntity(dtos, HttpStatus.OK);
    }
	
	@GetMapping(path = "/get/sci", produces = "application/json")
    public @ResponseBody ResponseEntity<List<SciFieldDto>> getscientificFields() {
		
		List<Scientificfield> scientificAreas = scientificService.getAll();
		List<SciFieldDto> scientificDTO = new ArrayList<>();
		
		for(Scientificfield c : scientificAreas){
			scientificDTO.add(scientificService.mapToDTO(c));
		}
		
		return new ResponseEntity<>(scientificDTO, HttpStatus.OK);
    }	
	
	@PostMapping(path = "/post/{taskId}", produces = "application/json")
    public @ResponseBody ResponseEntity post(@RequestBody List<FieldDto> dto, @PathVariable String taskId) {
		HashMap<String, Object> map = this.mapListToDto(dto);
		
		    // list all running/unsuspended instances of the process
//		    ProcessInstance processInstance =
//		        runtimeService.createProcessInstanceQuery()
//		            .processDefinitionKey("Process_1")
//		            .active() // we only want the unsuspended process instances
//		            .list().get(0);
		
//			Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list().get(0);	
		
		Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
		String processInstanceId = task.getProcessInstanceId();
		runtimeService.setVariable(processInstanceId, "registration", dto);
		formService.submitTaskForm(taskId, map);
		
        return new ResponseEntity<>(HttpStatus.OK);
    }
	
	@PostMapping(path = "/tasks/complete/{taskId}", produces = "application/json")
    public @ResponseBody ResponseEntity<List<TaskDto>> complete(@PathVariable String taskId) {
		Task taskTemp = taskService.createTaskQuery().taskId(taskId).singleResult();
		taskService.complete(taskId);
		List<Task> tasks = taskService.createTaskQuery().processInstanceId(taskTemp.getProcessInstanceId()).list();
		List<TaskDto> dtos = new ArrayList<TaskDto>();
		for (Task task : tasks) {
			TaskDto t = new TaskDto(task.getId(), task.getName(), task.getAssignee());
			dtos.add(t);
		}
		
        return new ResponseEntity<List<TaskDto>>(dtos, HttpStatus.OK);
    }
	
	private HashMap<String, Object> mapListToDto(List<FieldDto> list)
	{
		HashMap<String, Object> map = new HashMap<String, Object>();
		for(FieldDto temp : list){
			map.put(temp.getFieldId(), temp.getFieldValue());
		}
		
		return map;
	}
	
	@PostMapping(path = "/addSciFieldForUser/{taskId}", produces = "application/json")
    public @ResponseBody ResponseEntity<Object> addSciFieldForUser(@RequestBody List<FieldDto> dto, @PathVariable String taskId) {
		
		Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
		String processInstanceId = task.getProcessInstanceId();
		runtimeService.setVariable(processInstanceId, "sciFields", dto);
		taskService.complete(taskId);
        return new ResponseEntity<>(HttpStatus.OK);
    }
	

}
