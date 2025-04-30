<template>
  <div id="app" class="container">
    <b-container>
      <b-row>
        <b-col style="padding: 0">
          <b-navbar toggleable="lg" type="dark">
            <b-collapse id="nav-collapse" is-nav>
              <b-navbar-nav>
                <router-link to="/" class="nav-link">Home</router-link>
                <router-link to="/treatment-comparison" class="nav-link">Treatments</router-link>
                <router-link to="/group-comparison" class="nav-link">Patient Groups</router-link>
                <router-link to="/longitudinal-comparison" class="nav-link">Longitudinal</router-link>
                <router-link to="/subsets" class="nav-link nav-link-subpages">Subsets</router-link>
              </b-navbar-nav>

              <!-- Right aligned nav items -->
              <b-navbar-nav class="ml-auto">
                <b-btn v-if="!isConnected" @click="connectConversation" style="margin-right: 10px" class="ai-agent">
                  <b-icon icon="lightning"></b-icon>
                  Connect AI Agent
                </b-btn>
                <b-btn v-if="isConnected" @mousedown="startRecording" @mouseup="stopRecording" style="margin-right: 10px" class="ai-agent">
                  <b-icon icon="mic"></b-icon>
                  <span v-if="!isRecording">Push to talk</span>
                  <span v-if="isRecording">Release to send</span>
                </b-btn>
                <b-btn v-if="isConnected" @click="disconnectConversation" style="margin-right: 10px; font-size: 0.9em" class="">Disconnect</b-btn>
                <b-navbar-brand href="#">Snolytical (prototype)</b-navbar-brand>
              </b-navbar-nav>
            </b-collapse>
          </b-navbar>
        </b-col>
      </b-row>
      <router-view></router-view>
    </b-container>
  </div>
</template>

<script>

import {nextTick} from "vue";
import {RealtimeClient} from '@openai/realtime-api-beta';
import {WavRecorder, WavStreamPlayer} from '@/lib/wavtools';
import {instructions} from '/src/utils/conversation_config';
import OutcomeComparison from "@/components/OutcomeComparison.vue";

export default {
  name: 'App',
  data() {
    return {
      initialPhraseToAgent: 'Hello',
      isConnected: false,
      isRecording: false,
      client: null,
      wavRecorder: null,
      wavStreamPlayer: null,
    }
  },
  mounted() {
    // this.navigateTo('/group-comparison')
    // nextTick(() => {
    //   this.addOutcomeFindingCriteria('covid pne')
    // })
  },
  methods: {
    getOutcomeComparison() {
      const routeComponent = this.$route.matched.find(match => match.components.default === OutcomeComparison);
      return routeComponent.instances.default;
    },
    async setCohortGender(gender) {
      const outcomeComparison = this.getOutcomeComparison();
      if (gender === 'ALL') {
        gender = ''
      }
      outcomeComparison.cohortCriteria.gender = gender
    },
    async addCohortFindingCriteria(findingTerm) {
      try {
        const outcomeComparison = this.getOutcomeComparison();
        const cohortSelection = outcomeComparison.$children[0]
        cohortSelection.addEventCriterion('Clinical Finding', '<404684003')
        nextTick(() => {
          const eventCriterions = cohortSelection.$children[3]
          const eventCriterion = eventCriterions.$children[eventCriterions.$children.length - 1];
          eventCriterion.$data.searchInput = findingTerm
        })
      } catch (e) {
        console.log(e)
        throw e
      }
    },
    async addOutcomeFindingCriteria(findingTerm) {
      try {
        const outcomeComparison = this.getOutcomeComparison();
        outcomeComparison.addOutcome('Clinical Finding', '<404684003')
        nextTick(() => {
          console.log('outcomeComparison', outcomeComparison)
          const outcomes = outcomeComparison.$children[outcomeComparison.$children.length - 1]
          const outcome = outcomes.$children[outcomes.$children.length - 1];
          outcome.$data.searchInput = findingTerm
        })
      } catch (e) {
        console.log(e)
        throw e
      }
    },
    async navigateTo(routePath) {
      console.log('Route selected', routePath)
      try {
        if (this.$route.path !== routePath) {
          await this.$router.push(routePath);
        } else {
          console.log('Already at this route', routePath)
        }
      } catch (e) {
        console.log(e)
        throw e
      }
    },
    async connectConversation() {
      console.log('Connecting conversation...');

      const apiKey = 'YOUR_API_KEY_HERE'
      // const apiKey = prompt('OpenAI API Key');
      if (apiKey) {
        this.client = new RealtimeClient({
          apiKey: apiKey,
          dangerouslyAllowAPIKeyInBrowser: true,
        });

        this.wavRecorder = new WavRecorder({ sampleRate: 24000 });
        this.wavStreamPlayer = new WavStreamPlayer({ sampleRate: 24000 });

        try {
          // Connect to microphone
          await this.wavRecorder.begin();

          // Connect to audio output
          await this.wavStreamPlayer.connect();

          // Connect to realtime API
          await this.client.connect();

          // Set instructions and voice
          this.client.updateSession({instructions: instructions, voice: 'shimmer' });
          // Set transcription, otherwise we don't get user transcriptions back
          // this.client.updateSession({ input_audio_transcription: { model: 'whisper-1' } });

          this.client.addTool(
              {
                name: 'select_group_comparison',
                description:
                    'This function selects the patient group comparison report.'
              },
              () => {
                this.navigateTo('/group-comparison')
              },
          )
          this.client.addTool(
              {
                name: 'add_cohort_gender',
                description:
                    'Add cohort gender',
                // 'Select a gender for the patient cohort',
                parameters: {
                  type: 'object',
                  properties: {
                    gender: {
                      type: 'string',
                      description: 'Value can be "MALE", "FEMALE" or "ALL" only.',
                    },
                  },
                  required: ['value'],
                }              },
              (params) => {
                //
                try {
                  this.setCohortGender(params.gender)
                } catch (e) {
                  console.log(e);
                  throw e
                }
              },
          )
          this.client.addTool(
              {
                name: 'add_cohort_criteria_clinical_finding',
                description:
                    'Add a criteria to the cohort definition of type clinical finding. ' +
                    'This will be a clinical finding or disorder from SNOMED CT using just the term.',
                parameters: {
                  type: 'object',
                  properties: {
                    findingTerm: {
                      type: 'string',
                      description: 'Value can be the term of a finding or disorder that could be found in SNOMED CT. ' +
                          'For example "asthma", "hypertension", "BRCA1 mutation detected".',
                    },
                  },
                  required: ['value'],
                }              },
              (params) => {
                //
                try {
                  this.addCohortFindingCriteria(params.findingTerm)
                } catch (e) {
                  console.log(e);
                  throw e
                }
              },
          )
          this.client.addTool(
              {
                name: 'add_outcome_clinical_finding',
                description:
                    'Add an outcome to measure. ' +
                    'This will be a clinical finding or disorder from SNOMED CT using just the term. ',
                parameters: {
                  type: 'object',
                  properties: {
                    findingTerm: {
                      type: 'string',
                      description: 'Value can be the term of a finding or disorder that could be found in SNOMED CT. ' +
                          'For example "asthma", "hypertension", "BRCA1 mutation detected".',
                    },
                  },
                  required: ['value'],
                }              },
              (params) => {
                //
                try {
                  this.addOutcomeFindingCriteria(params.findingTerm)
                } catch (e) {
                  console.log(e);
                  throw e
                }
              },
          )

          this.client.sendUserMessageContent([
            {
              type: 'input_text',
              text: this.initialPhraseToAgent,
            },
          ]);

          this.client.on('conversation.updated', async ({ item, delta }) => {
            // console.log('conversation.updated', item)
            const items = this.client.conversation.getItems();
            if (delta?.audio) {
              this.wavStreamPlayer.add16BitPCM(delta.audio, item.id);
            }
            if (item.status === 'completed' && item.formatted.audio?.length) {
              const wavFile = await WavRecorder.decode(
                  item.formatted.audio,
                  24000,
                  24000
              );
              item.formatted.file = wavFile;
            }
            // setItems(items);
          });

          console.log('Connected successfully!');
          this.isConnected = true;
        } catch (error) {
          console.error('Failed to connect:', error);
        }
      } else {
        console.warn('No API key provided.');
      }
    },
    async disconnectConversation() {
      console.log('Disconnecting conversation...');
      try {
        if (this.client) {
          this.client.disconnect()
        }
        if (this.wavRecorder) {
          this.wavRecorder.end()
        }
        if (this.wavStreamPlayer) {
          this.wavStreamPlayer.interrupt()
        }
        this.isConnected = false;
        console.log('Disconnected successfully!');
      } catch (error) {
        console.error('Failed to disconnect:', error);
      }
    },
    async startRecording() {

      // TODO: Use this to check connection .. seems to be dropping after first question
      // this.client.isConnected

      this.isRecording = true;
      const trackSampleOffset = await this.wavStreamPlayer.interrupt();
      if (trackSampleOffset?.trackId) {
        const { trackId, offset } = trackSampleOffset;
        this.client.cancelResponse(trackId, offset);
      }
      await this.wavRecorder.record((data) => this.client.appendInputAudio(data.mono));
    },
    async stopRecording() {
      this.isRecording = false;
      await this.wavRecorder.pause();
      this.client.createResponse();
    },
  },
};
</script>

<style>
#app {
  font-family: Avenir, Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  text-align: center;
  color: #2c3e50;
}
.navbar {
  background-color: #00A9E0;
  margin-bottom: 10px;
}
.navbar .navbar-nav {
  font-weight: 500;
}
#app .navbar .navbar-nav a.router-link-exact-active, #app .navbar a.nav-link-subpages.router-link-active {
    color: rgba(255, 255, 255, 0.9);
}
#app .btn-primary {
  color: #fff;
  background-color: #00A9E0;
  border-color: #00A9E0;
  /* background-color: #6DBBA1;
  border-color: #6DBBA1; */
}
#app .removeBtn {
  line-height: 10px;
  background-color: white;
  opacity: 75%;
  color: #CE0037;
  border: 0px;
  float: right;
  padding: 0;
}

#app .ai-agent {
  border: 2px solid #FF924C;
  background-color: #00A9E0;
  font-size: 0.9em;
}
</style>
