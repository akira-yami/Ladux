package org.akira.ladux.notification.service;

import org.akira.ladux.notification.dto.request.ContactRequest;

public interface ContactService {

    void sendContactMessage(ContactRequest request);
}