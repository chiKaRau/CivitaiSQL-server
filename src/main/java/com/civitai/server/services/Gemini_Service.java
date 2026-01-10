package com.civitai.server.services;

import java.util.List;

import com.civitai.server.services.impl.Gemini_Service_impl.CharacterMatchInput;
import com.civitai.server.services.impl.Gemini_Service_impl.CharacterTitleMatch;

public interface Gemini_Service {

    public List<CharacterTitleMatch> matchCharactersToTitles(List<CharacterMatchInput> inputs);

}
