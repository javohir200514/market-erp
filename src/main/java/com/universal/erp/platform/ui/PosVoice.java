package com.universal.erp.platform.ui;

import org.springframework.stereotype.Component;

import com.vaadin.flow.component.UI;

import jakarta.annotation.PostConstruct;

@Component
public class PosVoice {

    private static volatile PosVoice instance;

    private final PosSettings settings;

    public PosVoice(PosSettings settings) {
        this.settings = settings;
    }

    @PostConstruct
    void register() {
        instance = this;
    }


    private static final String JS = """
    if(!window.__uzSay){
      const q=[];
      let busy=false;

      const findVoice=()=>{
        const vs=window.speechSynthesis
          ? window.speechSynthesis.getVoices() : [];

        return vs.find(v =>
          (v.lang || '').toLowerCase()
            .replace('_','-').startsWith('uz')
        ) || null;
      };

      const nativeSpeak=(it, done)=>{
        const v=findVoice();

        if(!v || !window.speechSynthesis){
          done();
          return;
        }

        const u=new SpeechSynthesisUtterance(it.t);
        u.voice=v;
        u.lang=v.lang;
        u.volume=Math.max(0,Math.min(1,it.v));
        u.rate=Math.max(0.7,Math.min(1.3,it.r));
        u.onend=done;
        u.onerror=done;

        window.speechSynthesis.speak(u);
      };

      const next=()=>{
        if(busy) return;

        const it=q.shift();
        if(!it) return;

        busy=true;
        let finished=false;

        const done=()=>{
          if(finished) return;
          finished=true;
          busy=false;
          next();
        };

        const fallback=()=>{
          nativeSpeak(it,done);
        };

        // Avval tashqi sifatli TTS
        if(it.u && it.u.trim()){
          try{
            const url=it.u.replace(
              '{text}',
              encodeURIComponent(it.t)
            );

            const a=new Audio(url);
            a.volume=Math.max(0,Math.min(1,it.v));
            a.playbackRate=Math.max(0.7,Math.min(1.3,it.r));

            let usedFallback=false;

            const fail=()=>{
              if(usedFallback || finished) return;
              usedFallback=true;
              fallback();
            };

            a.onended=done;
            a.onerror=fail;

            setTimeout(()=>{
              if(!finished && !usedFallback && a.paused){
                fail();
              }
            },8000);

            a.play().catch(fail);
            return;

          }catch(e){
            fallback();
            return;
          }
        }

        // Tashqi TTS bo'lmasa, faqat o'zbekcha brauzer ovozi
        nativeSpeak(it,done);
      };

      window.__uzSay=(t,v,r,u)=>{
        q.push({
          t:String(t || ''),
          v:Number(v) || 1,
          r:Number(r) || 1,
          u:String(u || '')
        });

        if(q.length>10) q.shift();
        next();
      };
    }

    window.__uzSay($0,$1,$2,$3);
    """;


    /** Kompaniya sozlamasiga ko'ra aytadi (o'chiq bo'lsa jim). */
    public static void say(UI ui, String company, String text) {
        PosVoice v = instance;
        if (v == null || ui == null || text == null || text.isBlank()) {
            return;
        }
        PosSettings.Values s = v.settings.get(company);
        if (s.voice()) {
            sayWith(ui, text, s.volume(), s.rate(), s.ttsUrl());
        }
    }

    /** Sozlamalar sahifasidagi sinov uchun (saqlanmagan qiymatlar bilan). */
    public static void sayWith(UI ui, String text, double volume, double rate, String ttsUrl) {
        ui.getPage().executeJs(JS, text, volume, rate, ttsUrl == null ? "" : ttsUrl);
    }
}