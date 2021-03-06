package com.popov.t04jh.web.rest;

import com.popov.t04jh.T04JhApp;
import com.popov.t04jh.domain.Presence;
import com.popov.t04jh.repository.PresenceRepository;
import com.popov.t04jh.service.PresenceService;
import com.popov.t04jh.web.rest.errors.ExceptionTranslator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Validator;

import javax.persistence.EntityManager;
import java.util.List;

import static com.popov.t04jh.web.rest.TestUtil.createFormattingConversionService;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the {@link PresenceResource} REST controller.
 */
@SpringBootTest(classes = T04JhApp.class)
public class PresenceResourceIT {

    private static final String DEFAULT_ID_NAGEUR_ABS = "AAAAAAAAAA";
    private static final String UPDATED_ID_NAGEUR_ABS = "BBBBBBBBBB";

    @Autowired
    private PresenceRepository presenceRepository;

    @Autowired
    private PresenceService presenceService;

    @Autowired
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Autowired
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    @Autowired
    private ExceptionTranslator exceptionTranslator;

    @Autowired
    private EntityManager em;

    @Autowired
    private Validator validator;

    private MockMvc restPresenceMockMvc;

    private Presence presence;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        final PresenceResource presenceResource = new PresenceResource(presenceService);
        this.restPresenceMockMvc = MockMvcBuilders.standaloneSetup(presenceResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setControllerAdvice(exceptionTranslator)
            .setConversionService(createFormattingConversionService())
            .setMessageConverters(jacksonMessageConverter)
            .setValidator(validator).build();
    }

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Presence createEntity(EntityManager em) {
        Presence presence = new Presence()
            .idNageurAbs(DEFAULT_ID_NAGEUR_ABS);
        return presence;
    }
    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Presence createUpdatedEntity(EntityManager em) {
        Presence presence = new Presence()
            .idNageurAbs(UPDATED_ID_NAGEUR_ABS);
        return presence;
    }

    @BeforeEach
    public void initTest() {
        presence = createEntity(em);
    }

    @Test
    @Transactional
    public void createPresence() throws Exception {
        int databaseSizeBeforeCreate = presenceRepository.findAll().size();

        // Create the Presence
        restPresenceMockMvc.perform(post("/api/presences")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(presence)))
            .andExpect(status().isCreated());

        // Validate the Presence in the database
        List<Presence> presenceList = presenceRepository.findAll();
        assertThat(presenceList).hasSize(databaseSizeBeforeCreate + 1);
        Presence testPresence = presenceList.get(presenceList.size() - 1);
        assertThat(testPresence.getIdNageurAbs()).isEqualTo(DEFAULT_ID_NAGEUR_ABS);
    }

    @Test
    @Transactional
    public void createPresenceWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = presenceRepository.findAll().size();

        // Create the Presence with an existing ID
        presence.setId(1L);

        // An entity with an existing ID cannot be created, so this API call must fail
        restPresenceMockMvc.perform(post("/api/presences")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(presence)))
            .andExpect(status().isBadRequest());

        // Validate the Presence in the database
        List<Presence> presenceList = presenceRepository.findAll();
        assertThat(presenceList).hasSize(databaseSizeBeforeCreate);
    }


    @Test
    @Transactional
    public void getAllPresences() throws Exception {
        // Initialize the database
        presenceRepository.saveAndFlush(presence);

        // Get all the presenceList
        restPresenceMockMvc.perform(get("/api/presences?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(presence.getId().intValue())))
            .andExpect(jsonPath("$.[*].idNageurAbs").value(hasItem(DEFAULT_ID_NAGEUR_ABS)));
    }
    
    @Test
    @Transactional
    public void getPresence() throws Exception {
        // Initialize the database
        presenceRepository.saveAndFlush(presence);

        // Get the presence
        restPresenceMockMvc.perform(get("/api/presences/{id}", presence.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.id").value(presence.getId().intValue()))
            .andExpect(jsonPath("$.idNageurAbs").value(DEFAULT_ID_NAGEUR_ABS));
    }

    @Test
    @Transactional
    public void getNonExistingPresence() throws Exception {
        // Get the presence
        restPresenceMockMvc.perform(get("/api/presences/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void updatePresence() throws Exception {
        // Initialize the database
        presenceService.save(presence);

        int databaseSizeBeforeUpdate = presenceRepository.findAll().size();

        // Update the presence
        Presence updatedPresence = presenceRepository.findById(presence.getId()).get();
        // Disconnect from session so that the updates on updatedPresence are not directly saved in db
        em.detach(updatedPresence);
        updatedPresence
            .idNageurAbs(UPDATED_ID_NAGEUR_ABS);

        restPresenceMockMvc.perform(put("/api/presences")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(updatedPresence)))
            .andExpect(status().isOk());

        // Validate the Presence in the database
        List<Presence> presenceList = presenceRepository.findAll();
        assertThat(presenceList).hasSize(databaseSizeBeforeUpdate);
        Presence testPresence = presenceList.get(presenceList.size() - 1);
        assertThat(testPresence.getIdNageurAbs()).isEqualTo(UPDATED_ID_NAGEUR_ABS);
    }

    @Test
    @Transactional
    public void updateNonExistingPresence() throws Exception {
        int databaseSizeBeforeUpdate = presenceRepository.findAll().size();

        // Create the Presence

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restPresenceMockMvc.perform(put("/api/presences")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(presence)))
            .andExpect(status().isBadRequest());

        // Validate the Presence in the database
        List<Presence> presenceList = presenceRepository.findAll();
        assertThat(presenceList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    public void deletePresence() throws Exception {
        // Initialize the database
        presenceService.save(presence);

        int databaseSizeBeforeDelete = presenceRepository.findAll().size();

        // Delete the presence
        restPresenceMockMvc.perform(delete("/api/presences/{id}", presence.getId())
            .accept(TestUtil.APPLICATION_JSON_UTF8))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        List<Presence> presenceList = presenceRepository.findAll();
        assertThat(presenceList).hasSize(databaseSizeBeforeDelete - 1);
    }
}
