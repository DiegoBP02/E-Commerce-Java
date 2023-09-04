package com.example.demo.services;

import com.example.demo.ApplicationConfigTest;
import com.example.demo.services.exceptions.EmailSendException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class EmailServiceTest extends ApplicationConfigTest {

    @Autowired
    private EmailService emailService;

    @MockBean
    private JavaMailSender javaMailSender;

    @Test
    void givenEmailAndSubjectAndContent_whenSendEmail_thenSendEmail() {
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendEmail("email", "subject", "content");

        verify(javaMailSender, times(1)).createMimeMessage();
        verify(javaMailSender, times(1)).send(mimeMessage);
    }

    @Test
    void givenError_whenSendEmail_thenThrowEmailSendException() {
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
        try (MockedConstruction<MimeMessageHelper> mocked =
                     mockConstructionWithAnswer(MimeMessageHelper.class, invocation -> {
                         throw new MessagingException();
                     })) {
            assertThrows(EmailSendException.class, () ->
                    emailService.sendEmail("email", "subject", "content")
            );
        }

        verify(javaMailSender, times(1)).createMimeMessage();
        verify(javaMailSender, never()).send(mimeMessage);
    }

}