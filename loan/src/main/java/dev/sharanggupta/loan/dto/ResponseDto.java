package dev.sharanggupta.loan.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ResponseDto {

    private String statusCode;

    private String statusMessage;
}
