package com.minicommerce.address.adapter.in.web;

import com.minicommerce.address.application.port.in.ManageAddressUseCase;
import com.minicommerce.address.application.port.in.ManageAddressUseCase.NewAddress;
import com.minicommerce.address.domain.AddressNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/addresses")
public class AddressController {

    private final ManageAddressUseCase manageAddressUseCase;

    public AddressController(ManageAddressUseCase manageAddressUseCase) {
        this.manageAddressUseCase = manageAddressUseCase;
    }

    @GetMapping
    List<AddressResponse> list(HttpServletRequest httpRequest) {
        String customerId = (String) httpRequest.getAttribute("authenticatedUserId");
        return manageAddressUseCase.list(customerId).stream()
                .map(AddressResponse::from)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    AddressResponse add(@Valid @RequestBody SaveAddressRequest request, HttpServletRequest httpRequest) {
        String customerId = (String) httpRequest.getAttribute("authenticatedUserId");
        NewAddress command = new NewAddress(
                request.label(), request.name(), request.phone(), request.address1(), request.address2());
        return AddressResponse.from(manageAddressUseCase.add(customerId, command));
    }

    @DeleteMapping("/{addressId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void remove(@PathVariable String addressId, HttpServletRequest httpRequest) {
        String customerId = (String) httpRequest.getAttribute("authenticatedUserId");
        manageAddressUseCase.remove(customerId, addressId);
    }

    @PutMapping("/{addressId}/default")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void setDefault(@PathVariable String addressId, HttpServletRequest httpRequest) {
        String customerId = (String) httpRequest.getAttribute("authenticatedUserId");
        manageAddressUseCase.setDefault(customerId, addressId);
    }

    @ExceptionHandler(AddressNotFoundException.class)
    ProblemDetail handleNotFound(AddressNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setType(URI.create("https://mini-commerce.local/problems/address-not-found"));
        problem.setTitle("Address not found");
        return problem;
    }
}
