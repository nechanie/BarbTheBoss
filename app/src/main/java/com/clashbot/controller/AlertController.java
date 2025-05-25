package com.clashbot.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.clashbot.discordbot.embeds.AlertEmbeds.AllyThreeStarAttackEmbed;
import com.clashbot.discordbot.embeds.AlertEmbeds.AttackReminderEmbed;
import com.clashbot.discordbot.embeds.AlertEmbeds.LeadChangeEmbed;
import com.clashbot.discordbot.embeds.AlertEmbeds.PerfectWarEmbed;
import com.clashbot.discordbot.embeds.AlertEmbeds.SignupReminderEmbed;
import com.clashbot.service.AlertService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/alerts")
public class AlertController {
    
    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @PostMapping("/attack-reminder")
    public Mono<ResponseEntity<Object>> receiveAttackReminder(@RequestBody AttackReminderEmbed payload) {
        return alertService.sendAlert(payload, alertService::createEmbed)
            .thenReturn(ResponseEntity.ok().build())
            .onErrorResume(e -> Mono.just(ResponseEntity.internalServerError().build()));
    }

    @PostMapping("/signup-reminder")
    public Mono<ResponseEntity<Object>> receiveSignupReminder(@RequestBody SignupReminderEmbed payload) {
        return alertService.sendAlert(payload, alertService::createEmbed)
            .thenReturn(ResponseEntity.ok().build())
            .onErrorResume(e -> Mono.just(ResponseEntity.internalServerError().build()));
    }

    @PostMapping("/events/lead-change")
    public Mono<ResponseEntity<Object>> receiveLeadChangeEvent(@RequestBody LeadChangeEmbed payload) {
        return alertService.sendAlert(payload, alertService::createEmbed)
            .thenReturn(ResponseEntity.ok().build())
            .onErrorResume(e -> Mono.just(ResponseEntity.internalServerError().build()));
    }

    @PostMapping("/events/ally-three-star-attack")
    public Mono<ResponseEntity<Object>> receiveAllyThreeStarAttackEvent(@RequestBody AllyThreeStarAttackEmbed payload) {
        return alertService.sendAlert(payload, alertService::createEmbed)
            .thenReturn(ResponseEntity.ok().build())
            .onErrorResume(e -> Mono.just(ResponseEntity.internalServerError().build()));
    }

    @PostMapping("/events/perfect-war")
    public Mono<ResponseEntity<Object>> receivePerfectWarEvent(@RequestBody PerfectWarEmbed payload) {
        return alertService.sendAlert(payload, alertService::createEmbed)
            .thenReturn(ResponseEntity.ok().build())
            .onErrorResume(e -> Mono.just(ResponseEntity.internalServerError().build()));
    }
}
