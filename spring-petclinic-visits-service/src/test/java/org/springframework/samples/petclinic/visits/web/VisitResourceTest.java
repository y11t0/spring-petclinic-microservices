package org.springframework.samples.petclinic.visits.web;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.samples.petclinic.visits.model.Visit;
import org.springframework.samples.petclinic.visits.model.VisitRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;


import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VisitResource.class)
@ActiveProfiles("test")
class VisitResourceTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    VisitRepository visitRepository;

    @Test
    void shouldFetchVisits() throws Exception {
        given(visitRepository.findByPetIdIn(asList(111, 222)))
            .willReturn(
                asList(
                    Visit.VisitBuilder.aVisit()
                        .id(1)
                        .petId(111)
                        .build(),
                    Visit.VisitBuilder.aVisit()
                        .id(2)
                        .petId(222)
                        .build(),
                    Visit.VisitBuilder.aVisit()
                        .id(3)
                        .petId(222)
                        .build()
                )
            );

        mvc.perform(get("/pets/visits?petId=111,222"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].id").value(1))
            .andExpect(jsonPath("$.items[1].id").value(2))
            .andExpect(jsonPath("$.items[2].id").value(3))
            .andExpect(jsonPath("$.items[0].petId").value(111))
            .andExpect(jsonPath("$.items[1].petId").value(222))
            .andExpect(jsonPath("$.items[2].petId").value(222));
    }

    @Test
    void shouldCreateVisitWithVetId() throws Exception {
        given(visitRepository.save(any(Visit.class))).willAnswer(invocation -> {
            Visit visit = invocation.getArgument(0);
            visit.setId(10);
            return visit;
        });

        mvc.perform(post("/owners/1/pets/111/visits")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"date\":\"2024-05-06\",\"description\":\"checkup\",\"vetId\":3}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(10))
            .andExpect(jsonPath("$.petId").value(111))
            .andExpect(jsonPath("$.vetId").value(3));

        ArgumentCaptor<Visit> saved = ArgumentCaptor.forClass(Visit.class);
        verify(visitRepository).save(saved.capture());
        assertEquals(3, saved.getValue().getVetId());
    }

    @Test
    void shouldCreateVisitWithoutVetId() throws Exception {
        given(visitRepository.save(any(Visit.class))).willAnswer(invocation -> invocation.getArgument(0));

        mvc.perform(post("/owners/1/pets/111/visits")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"date\":\"2024-05-06\",\"description\":\"checkup\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.vetId").doesNotExist());
    }

    @Test
    void shouldReturnVetIdWhenReadingPetVisits() throws Exception {
        given(visitRepository.findByPetId(111))
            .willReturn(
                asList(
                    Visit.VisitBuilder.aVisit()
                        .id(1)
                        .petId(111)
                        .vetId(3)
                        .build(),
                    Visit.VisitBuilder.aVisit()
                        .id(2)
                        .petId(111)
                        .build()
                )
            );

        mvc.perform(get("/owners/1/pets/111/visits"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].vetId").value(3))
            .andExpect(jsonPath("$[1].vetId").doesNotExist());
    }

    @Test
    void shouldReturnVetIdWhenReadingVisitsByPetIds() throws Exception {
        given(visitRepository.findByPetIdIn(asList(111, 222)))
            .willReturn(
                asList(
                    Visit.VisitBuilder.aVisit()
                        .id(1)
                        .petId(111)
                        .vetId(4)
                        .build()
                )
            );

        mvc.perform(get("/pets/visits?petId=111,222"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].vetId").value(4));
    }
}
