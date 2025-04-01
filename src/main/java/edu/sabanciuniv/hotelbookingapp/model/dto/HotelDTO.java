package edu.sabanciuniv.hotelbookingapp.model.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HotelDTO {
    
    private Long id;
    
    @NotBlank(message = "Hotel name cannot be empty")
    private String name;
    
    @Valid
    @NotNull(message = "Address cannot be null")
    private AddressDTO addressDTO;
    
    @Valid
    @Builder.Default
    private List<RoomDTO> roomDTOs = new ArrayList<>();
    
    private String managerName;
    
    private Long managerId;
    
    // Method to ensure roomDTOs is never null
    public List<RoomDTO> getRoomDTOs() {
        if (roomDTOs == null) {
            roomDTOs = new ArrayList<>();
        }
        return roomDTOs;
    }
    
    // Method to calculate the price per night based on the first room type available
    // Used in the BookingController
    public double getPricePerNight() {
        if (roomDTOs != null && !roomDTOs.isEmpty()) {
            return roomDTOs.stream()
                .filter(room -> room.getRoomCount() > 0)
                .findFirst()
                .map(RoomDTO::getPricePerNight)
                .orElse(0.0);
        }
        return 0.0;
    }
}
