package com.nekocafe.service;

import com.nekocafe.common.ApiException;
import com.nekocafe.dto.CatHealthRequest;
import com.nekocafe.entity.CatHealthRecord;
import com.nekocafe.mapper.CatHealthRecordMapper;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class CatService {

    private final CatHealthRecordMapper catHealthRecordMapper;

    public CatService(CatHealthRecordMapper catHealthRecordMapper) {
        this.catHealthRecordMapper = catHealthRecordMapper;
    }

    public Map<String, Object> addHealthRecord(CatHealthRequest request) {
        if (request == null || request.catId() == null) {
            throw ApiException.badRequest("catId is required");
        }
        CatHealthRecord record = new CatHealthRecord();
        record.setCatId(request.catId());
        record.setWeightKg(request.weightKg());
        record.setVaccineNote(request.vaccineNote());
        record.setInteractionNote(request.interactionNote());
        catHealthRecordMapper.insert(record);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", record.getId());
        out.put("cat_id", record.getCatId());
        out.put("weight_kg", record.getWeightKg());
        out.put("vaccine_note", record.getVaccineNote());
        out.put("interaction_note", record.getInteractionNote());
        return out;
    }
}
