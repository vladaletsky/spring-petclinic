/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.samples.petclinic.owner;

import org.springframework.samples.petclinic.vet.VetRepository;
import org.springframework.samples.petclinic.visit.Visit;
import org.springframework.samples.petclinic.visit.VisitRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author Juergen Hoeller
 * @author Ken Krebs
 * @author Arjen Poutsma
 * @author Michael Isvy
 * @author Dave Syer
 */
@Controller
class VisitController {

	private final VisitRepository visits;

	private final PetRepository pets;

	private final VetRepository vetRepository;

	public VisitController(VisitRepository visits, PetRepository pets, VetRepository vetRepository) {
		this.visits = visits;
		this.pets = pets;
		this.vetRepository = vetRepository;
	}

	@InitBinder
	public void setAllowedFields(WebDataBinder dataBinder) {
		dataBinder.setDisallowedFields("id");
	}

	/**
	 * Called before each and every @RequestMapping annotated method. 2 goals: - Make sure
	 * we always have fresh data - Since we do not use the session scope, make sure that
	 * Pet object always has an id (Even though id is not part of the form fields)
	 *
	 * @param petId
	 * @return Pet
	 */
	@ModelAttribute("visit")
	public Visit loadPetWithVisit(@PathVariable("petId") int petId, Map<String, Object> model) {
		Pet pet = this.pets.findById(petId);
		pet.setVisitsInternal(this.visits.findByPetId(petId));
		model.put("pet", pet);
		Visit visit = new Visit();
		model.put("vets", vetRepository.findAll());
		pet.addVisit(visit);
		return visit;
	}


	// Spring MVC calls method loadPetWithVisit(...) before initNewVisitForm is called
	@GetMapping("/owners/*/pets/{petId}/visits/new")
	public String initNewVisitForm(@PathVariable("petId") int petId, Map<String, Object> model) {

		return "pets/createOrUpdateVisitForm";
	}

	// Spring MVC calls method loadPetWithVisit(...) before processNewVisitForm is called
	@PostMapping("/owners/{ownerId}/pets/{petId}/visits/new")
	public String processNewVisitForm(@Valid Visit visit, BindingResult result) {
		if (result.hasErrors()) {
			return "pets/createOrUpdateVisitForm";
		} else {
			this.visits.save(visit);
			return "redirect:/owners/{ownerId}";
		}
	}

	@GetMapping("/owners/{ownerId}/{petId}/visits/{visitId}/edit")
	public String initUpdateForm(@PathVariable("visitId") int visitId, @PathVariable("petId") int petId,
								 @PathVariable("ownerId") int ownerId, ModelMap model) {
		List<Visit> visitsByPetId = this.visits.findByPetId(petId);
		Optional<Visit> visit = visitsByPetId.stream().filter(item -> item.getId().equals(visitId)).findFirst();
		model.put("visit", visit.get());
		return "pets/createOrUpdateVisitForm";
	}

	@PostMapping("/owners/{ownerId}/{petId}/visits/{visitId}/edit")
	public String processUpdateForm(@Valid Visit visit, @PathVariable int visitId, BindingResult result, Owner owner, ModelMap model) {
		if (result.hasErrors()) {
			return "pets/createOrUpdateVisitForm";
		} else {
			visit.setId(visitId);
			this.visits.save(visit);
			return "redirect:/owners/{ownerId}";
		}
	}

	@GetMapping("/owners/{ownerId}/{petId}/visits/{visitId}/cancel")
	public String cancelVisit(@RequestBody(required = false) Visit visitFromForm, @PathVariable int visitId, @PathVariable int petId, BindingResult result, Owner owner, ModelMap model) {
		if (result.hasErrors()) {
			return "pets/createOrUpdateVisitForm";
		} else {
			List<Visit> visitsByPetId = visits.findByPetId(petId);
			Optional<Visit> visitOpt = visitsByPetId.stream().filter(item -> item.getId().equals(visitId)).findFirst();
			Visit visit = visitOpt.get();
			visit.setActive(false);
			visit.setId(visitId);
			this.visits.save(visit);
			return "redirect:/owners/{ownerId}";
		}
	}
}

