package pt.glsmanagement.platform.customer;

import org.springframework.stereotype.Component;

@Component
class PortugueseVatNumberValidator {

    boolean isStructurallyValid(String vatNumber) {
        if (vatNumber == null || !vatNumber.matches("[1-9][0-9]{8}")) {
            return false;
        }
        if ("999999990".equals(vatNumber)) {
            return false;
        }

        int weightedSum = 0;
        for (int index = 0; index < 8; index++) {
            weightedSum += Character.digit(vatNumber.charAt(index), 10) * (9 - index);
        }

        int checkDigit = 11 - weightedSum % 11;
        if (checkDigit >= 10) {
            checkDigit = 0;
        }
        return checkDigit == Character.digit(vatNumber.charAt(8), 10);
    }
}
