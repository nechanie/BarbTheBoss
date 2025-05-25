package com.clashbot.models.utils;

import com.clashbotbackend.dto.UpdateServerSettingsRequest;
import com.clashbotbackend.dto.UpdateWarNewsSettingsRequest;

public class ServerSettingsUpdateRequestContainer {
    
    private UpdateServerSettingsRequest updateServerSettingsRequest;

    private UpdateWarNewsSettingsRequest updateWarNewsSettingsRequest;

    public ServerSettingsUpdateRequestContainer(){
        setUpdateServerSettingsRequest(new UpdateServerSettingsRequest());
        setUpdateWarNewsSettingsRequest(new UpdateWarNewsSettingsRequest());
    }

    public void setUpdateServerSettingsRequest(UpdateServerSettingsRequest updateServerSettingsRequest) { this.updateServerSettingsRequest = updateServerSettingsRequest; }

    public UpdateServerSettingsRequest getUpdateServerSettingsRequest() { return this.updateServerSettingsRequest; }

    public void setUpdateWarNewsSettingsRequest(UpdateWarNewsSettingsRequest updateWarNewsSettingsRequest) { this.updateWarNewsSettingsRequest = updateWarNewsSettingsRequest; }

    public UpdateWarNewsSettingsRequest getUpdateWarNewsSettingsRequest() { return this.updateWarNewsSettingsRequest; }
}
