package com.nekocafe.web;

import com.nekocafe.common.ApiResponse;
import com.nekocafe.service.TableAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/tables")
@Tag(name = "Table Admin", description = "Table management queries")
public class TableController {

    private final TableAdminService tableAdminService;

    public TableController(TableAdminService tableAdminService) {
        this.tableAdminService = tableAdminService;
    }

    @Operation(summary = "List tables with availability")
    @GetMapping
    public ApiResponse listTables(@RequestParam(required = false) Long storeId,
                                  @RequestParam(required = false) String date,
                                  @RequestParam(required = false) String time,
                                  @RequestParam(required = false) Integer partySize,
                                  @RequestParam(required = false) String area,
                                  @RequestParam(required = false) String status,
                                  @RequestParam(required = false) Boolean availableOnly) {
        return ApiResponse.of(tableAdminService.listTables(storeId, date, time, partySize, area, status, availableOnly));
    }

    @Operation(summary = "Get table detail")
    @GetMapping("/{id}")
    public ApiResponse getTableDetail(@PathVariable Long id,
                                      @RequestParam(required = false) String date,
                                      @RequestParam(required = false) String time) {
        return ApiResponse.of(tableAdminService.getTableDetail(id, date, time));
    }

    @Operation(summary = "Create table")
    @PostMapping
    public ResponseEntity<ApiResponse> createTable(@RequestBody Map<String, Object> body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(tableAdminService.createTable(body)));
    }

    @Operation(summary = "Update table")
    @PutMapping("/{id}")
    public ApiResponse updateTable(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return ApiResponse.of(tableAdminService.updateTable(id, body));
    }

    @Operation(summary = "Delete table")
    @DeleteMapping("/{id}")
    public ApiResponse deleteTable(@PathVariable Long id) {
        return ApiResponse.of(tableAdminService.deleteTable(id));
    }
}
