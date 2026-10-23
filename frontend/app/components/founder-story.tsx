import {socialLinks} from "../../lib/social-links";
import styles from "./founder-story.module.css";

export default function FounderStory(){
 return <section className={styles.section} aria-labelledby="founder-story-title"><div className={styles.inner}>
  <div className={styles.identity}><div className="brand-logo" aria-label="Styled Somehow"><span>STYLED</span><span>SOMEHOW</span></div><span className={styles.caption}>A NOTE FROM THE FOUNDER</span></div>
  <div className={styles.story}>
   <h2 id="founder-story-title">For the tees you keep reaching for.</h2>
   <p>Some T-shirts become your favourites without you even realising it. You reach for them on busy mornings, lazy weekends and every “I’ll be ready in five” kind of day.</p>
   <p className={styles.highlight}>That’s what I’m building Styled Somehow for: comfortable oversized fits with enough personality to make them yours.</p>
   <p>I’m Khushi. After five years as a software developer, I left my job to build this brand around a simple idea—everyday clothes should feel comfortable, expressive and easy to wear.</p>
   <p>From finding the right T-shirts to testing prints and figuring out packaging, I’m putting thought into the details, so getting dressed takes a little less of yours.</p>
   <div className={styles.signature}>With love,<br/><strong>Khushi</strong></div>
   <a className={styles.journey} href={socialLinks.founderInstagram} target="_blank" rel="noopener noreferrer">Follow the journey <span aria-hidden="true">↗</span><span className={styles.srOnly}> on Instagram (opens in a new tab)</span></a>
  </div>
 </div></section>;
}
