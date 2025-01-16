package com.github.milomarten.santa_furret.service.diff;

import discord4j.discordjson.json.ApplicationCommandData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CommandDiffService {
    public Diff findDiff(List<ApplicationCommandData> original, List<ApplicationCommandRequest> newValues) {
        var originalLookup = original.stream()
                .collect(Collectors.toMap(
                        ApplicationCommandData::name, Function.identity()
                ));

        var added = new ArrayList<ApplicationCommandRequest>();
        var same = new ArrayList<ApplicationCommandData>();
        var subtracted = new ArrayList<Long>();

        for (var newValue : newValues) {
            ApplicationCommandData data = originalLookup.remove(newValue.name());
            if (data == null) {
                added.add(newValue);
            } else {
                same.add(data);
            }
        }

        originalLookup.values().forEach(acd -> subtracted.add(acd.id().asLong()));

        return new Diff(added, same, subtracted);
    }
}
