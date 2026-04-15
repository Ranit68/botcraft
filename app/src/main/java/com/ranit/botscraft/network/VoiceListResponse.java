package com.ranit.botscraft.network;

import java.util.List;

public class VoiceListResponse {
    public List<VoiceItem> voices;

    public static class VoiceItem {
        public String voice_id;
        public String name;
        public String preview_url;
        public Labels labels;

        public static class Labels {
            public String accent;
            public String description;
            public String age;
            public String gender;
            public String use_case;
        }
    }
}
