package com.github.milomarten.santa_furret.commands;

import com.github.milomarten.santa_furret.commands.parameter.Parameter;
import com.github.milomarten.santa_furret.commands.parameter.ParameterResolver;
import com.github.milomarten.santa_furret.models.SecretSantaOptions;
import com.github.milomarten.santa_furret.models.exception.EventInProgressException;
import com.github.milomarten.santa_furret.service.SecretSantaService;
import com.github.milomarten.santa_furret.util.Permission;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.function.Function;

import static java.time.temporal.ChronoField.*;

@Component
@RequiredArgsConstructor
public class CreateEventCommand implements SecretSantaCommand {
    private final SecretSantaService service;

    private static final ParameterResolver<ZoneId> TIMEZONE = Parameter.string("timezone")
            .convertLossy(ZoneId::of, "Invalid timezone: Should be tz format or a UTC offset")
            .required();
    private static final ParameterResolver<LocalDateTime> EVENT_START = Parameter.string("event-start")
            .convertLossy(DateTimeConverter.INSTANCE, "Invalid date: Must be yyyy-mm-dd hh:mm")
            .required();
    private static final ParameterResolver<LocalDateTime> REG_END = Parameter.string("registration-end")
            .convertLossy(DateTimeConverter.INSTANCE, "Invalid date: Must be yyyy-mm-dd hh:mm")
            .required();
    private static final ParameterResolver<LocalDateTime> GIFT_START = Parameter.string("gifting-start")
            .convertLossy(DateTimeConverter.INSTANCE, "Invalid date: Must be yyyy-mm-dd hh:mm")
            .required();
    private static final ParameterResolver<LocalDateTime> EVENT_END = Parameter.string("event-end")
            .convertLossy(DateTimeConverter.INSTANCE, "Invalid date: Must be yyyy-mm-dd hh:mm")
            .required();

    @Override
    public ApplicationCommandRequest getSpec() {
        return ApplicationCommandRequest.builder()
                .name("create")
                .description("Create a secret santa event!")
                .defaultMemberPermissions(Permission.MANAGE_GUILD.single())
                .addOption(ApplicationCommandOptionData.builder()
                        .name("event-start")
                        .type(ApplicationCommandOption.Type.STRING.getValue())
                        .description("The day and optional time the event begins. Format: yyyy/mm/dd hh:mm:ss")
                        .required(true)
                        .minLength(10).maxLength(19)
                        .build())
                .addOption(ApplicationCommandOptionData.builder()
                        .name("registration-end")
                        .type(ApplicationCommandOption.Type.STRING.getValue())
                        .description("The day and optional time registration for this event ends. Format: yyyy/mm/dd hh:mm:ss")
                        .required(true)
                        .minLength(10).maxLength(19)
                        .build())
                .addOption(ApplicationCommandOptionData.builder()
                        .name("gifting-start")
                        .type(ApplicationCommandOption.Type.STRING.getValue())
                        .description("The day and optional time that gifting for this event starts. Format: yyyy/mm/dd hh:mm:ss")
                        .required(true)
                        .minLength(10).maxLength(19)
                        .build())
                .addOption(ApplicationCommandOptionData.builder()
                        .name("event-end")
                        .type(ApplicationCommandOption.Type.STRING.getValue())
                        .description("The day and optional time that the event ends. Format: yyyy/mm/dd hh:mm:ss")
                        .required(true)
                        .minLength(10).maxLength(19)
                        .build())
                .addOption(ApplicationCommandOptionData.builder()
                        .name("timezone")
                        .type(ApplicationCommandOption.Type.STRING.getValue())
                        .description("Your timezone, either as tz, three-letter code, or as a UTC offset")
                        .required(true)
                        .maxLength(29)
                        .build())
                .build();
    }

    @Override
    public Response handleCommand(ChatInputInteractionEvent event) {
        var guildId = event.getInteraction().getGuildId();
        if (guildId.isEmpty()) {
            return Responses.ephemeral("To create an event, you must run this command from your server!");
        }
        var eventRunner = event.getInteraction().getUser().getId();

        var timezone = TIMEZONE.resolve(event);
        var eventStart = EVENT_START.resolve(event).atZone(timezone);
        var regEnd = REG_END.resolve(event).atZone(timezone);
        var giftStart = GIFT_START.resolve(event).atZone(timezone);
        var eventEnd = EVENT_END.resolve(event).atZone(timezone);

        if (!isInOrder(ZonedDateTime.now(), eventStart, regEnd, giftStart, eventEnd)) {
            return Responses.ephemeral("Your times are misaligned. From earliest to latest, the order should be: event-start, registration-end, gifting-start, event-end. " +
                    "Or you may be trying to make an event with dates in the past! Tsk tsk.");
        }

        var options = new SecretSantaOptions(
                eventStart.toInstant(),
                regEnd.toInstant(),
                giftStart.toInstant(),
                eventEnd.toInstant()
        );

        Mono<String> message = Mono.fromCallable(() -> service.createEvent(guildId.get(), eventRunner, options))
                .map(ss -> """
                        Your event has been created! The ID is %s, don't forget it!
                        The event will formally kick off at <t:%d:f>.
                        Your members may then register up until <t:%d:f>.
                        At that point, everyone will be assigned a gifter. I'll let everyone know who they got!
                        On <t:%d:f>, the gifts will be exchanged. Santas will have until <t:%d:f> to send their gift, \
                        at which point the event will end.
                        
                        Remember, I'm a quiet bot, so it's up to you to make the announcements! I'll try and remind \
                        you when your dates come to pass. Thank you for allowing me to help!
                        """.formatted(ss.getId(), ss.getEventStartTime().getEpochSecond(), ss.getRegistrationEndTime().getEpochSecond(),
                        ss.getGiftingStartTime().getEpochSecond(), ss.getEventEndTime().getEpochSecond()))
                .onErrorResume(e -> {
                    if (e instanceof EventInProgressException) { return Mono.just("An event is already in progress for this server. I can't conduct two at once."); }
                    else { return Mono.just(e.getMessage()); }
                });
        return Responses.delayedEphemeral(message);
    }

    private boolean isInOrder(ZonedDateTime... times) {
        for (int idx = 0; idx < times.length - 1; idx++) {
            if (!times[idx].isBefore(times[idx + 1])) {
                return false;
            }
        }
        return true;
    }

    private enum DateTimeConverter implements Function<String, LocalDateTime> {
        INSTANCE;

        private static final DateTimeFormatter FORMATTER = new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .append(DateTimeFormatter.ISO_LOCAL_DATE)
                .optionalStart()
                .appendLiteral(' ')
                .appendValue(HOUR_OF_DAY, 2)
                .appendLiteral(':')
                .appendValue(MINUTE_OF_DAY, 2)
                .optionalEnd()
                .toFormatter();

        @Override
        public LocalDateTime apply(String s) {
            var temp = FORMATTER.parseBest(s, LocalDateTime::from, LocalDate::from);
            if (temp instanceof LocalDate ld) { return ld.atStartOfDay(); }
            else {return (LocalDateTime) temp; }
        }
    }
}
